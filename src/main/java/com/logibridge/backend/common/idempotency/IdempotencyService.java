package com.logibridge.backend.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logibridge.backend.common.exception.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public <T> T executeIdempotent(
            String idempotencyKey,
            String endpoint,
            Long userId,
            Class<T> responseType,
            Supplier<T> action
    ) {
        Optional<IdempotencyKey> existing =
                idempotencyKeyRepository.findByKeyAndUserId(idempotencyKey, userId);

        if (existing.isPresent()) {
            log.info("Idempotency hit: key={} endpoint={} userId={}",
                    idempotencyKey, endpoint, userId);

            String json = existing.get().getResponseBody();

            if (json != null) {
                return deserialize(json, responseType);
            }

            log.warn("Idempotency key found but response not ready yet. key={} userId={}",
                    idempotencyKey, userId);

            return action.get();
        }

        IdempotencyKey record = IdempotencyKey.builder()
                .key(idempotencyKey)
                .endpoint(endpoint)
                .userId(userId)
                .responseStatus(200)
                .responseBody(null)
                .build();

        try {
            idempotencyKeyRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition on idempotency key={} userId={} — fetching stored response",
                    idempotencyKey, userId);

            return idempotencyKeyRepository.findByKeyAndUserId(idempotencyKey, userId)
                    .map(k -> {
                        String json = k.getResponseBody();
                        if (json == null) {
                            return action.get();
                        }
                        return deserialize(json, responseType);
                    })
                    .orElseThrow(() -> new DuplicateRequestException(
                            "Duplicate request detected for key: " + idempotencyKey));
        }

        T result = action.get();

        String serialized = serialize(result);

        idempotencyKeyRepository.findByKeyAndUserId(idempotencyKey, userId)
                .ifPresent(k -> {
                    k.updateResponse(200, serialized);
                    idempotencyKeyRepository.save(k);
                });

        return result;
    }

    private <T> String serialize(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize idempotency response", ex);
            throw new DuplicateRequestException("Failed to serialize response");
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize idempotency response", ex);
            throw new DuplicateRequestException(
                    "Duplicate request detected — could not restore response.");
        }
    }
}