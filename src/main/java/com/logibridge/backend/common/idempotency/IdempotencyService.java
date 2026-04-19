package com.logibridge.backend.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logibridge.backend.common.exception.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofMinutes(5);

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
        Optional<IdempotencyKey> existingOpt =
                idempotencyKeyRepository.findByKeyAndUserId(idempotencyKey, userId);

        if (existingOpt.isPresent()) {
            IdempotencyKey existing = existingOpt.get();


            if (existing.getExpiresAt() != null &&
                    existing.getExpiresAt().isBefore(Instant.now())) {

                log.info("Idempotency expired — deleting key={} userId={}",
                        idempotencyKey, userId);

                idempotencyKeyRepository.delete(existing);
            } else {
                log.info("Idempotency hit: key={} endpoint={} userId={}",
                        idempotencyKey, endpoint, userId);

                String json = existing.getResponseBody();

                if (json != null) {
                    return deserialize(json, responseType);
                }

                log.warn("Idempotency key found but response not ready yet. key={} userId={}",
                        idempotencyKey, userId);

                return action.get();
            }
        }


        IdempotencyKey record = IdempotencyKey.builder()
                .key(idempotencyKey)
                .endpoint(endpoint)
                .userId(userId)
                .responseStatus(200)
                .responseBody(null)
                .expiresAt(Instant.now().plus(TTL))
                .build();

        try {
            idempotencyKeyRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition on idempotency key={} userId={} — fetching stored response",
                    idempotencyKey, userId);

            return idempotencyKeyRepository.findByKeyAndUserId(idempotencyKey, userId)
                    .map(k -> {

                        if (k.getExpiresAt() != null &&
                                k.getExpiresAt().isBefore(Instant.now())) {

                            idempotencyKeyRepository.delete(k);
                            return action.get();
                        }

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