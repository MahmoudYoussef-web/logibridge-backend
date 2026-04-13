package com.logibridge.backend.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logibridge.backend.common.idempotency.IdempotencyKey;
import com.logibridge.backend.common.idempotency.IdempotencyKeyRepository;
import com.logibridge.backend.common.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock IdempotencyKeyRepository repository;
    @Mock ObjectMapper objectMapper;

    @InjectMocks IdempotencyService service;

    @Test
    void returns_stored_response_on_duplicate_key() throws Exception {
        String key = "test-key";
        Long userId = 1L;
        String storedJson = "{\"orderNumber\":\"ORD-001\"}";

        IdempotencyKey existing = mock(IdempotencyKey.class);
        when(existing.getResponseBody()).thenReturn(storedJson);
        when(repository.findByKeyAndUserId(key, userId)).thenReturn(Optional.of(existing));

        TestResponse expected = new TestResponse("ORD-001");
        when(objectMapper.readValue(storedJson, TestResponse.class)).thenReturn(expected);

        TestResponse result = service.executeIdempotent(
                key, "testEndpoint", userId, TestResponse.class, () -> {
                    throw new RuntimeException("Should not be called");
                }
        );

        assertThat(result.orderNumber()).isEqualTo("ORD-001");
        verify(repository, never()).save(any());
    }

    @Test
    void executes_action_and_saves_on_first_request() throws Exception {
        String key = "new-key";
        Long userId = 1L;
        String serialized = "{\"orderNumber\":\"ORD-002\"}";

        IdempotencyKey savedRecord = mock(IdempotencyKey.class);

        when(repository.findByKeyAndUserId(key, userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedRecord));

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn(serialized);

        TestResponse result = service.executeIdempotent(
                key, "testEndpoint", userId, TestResponse.class,
                () -> new TestResponse("ORD-002")
        );

        assertThat(result.orderNumber()).isEqualTo("ORD-002");
        verify(repository, atLeastOnce()).save(any());
    }

    record TestResponse(String orderNumber) {}
}