package com.logibridge.backend.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logibridge.backend.common.exception.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService {

    private static final String KEY_PREFIX = "idempotency:";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T executeIdempotent(
            String idempotencyKey,
            Long userId,
            Class<T> responseType,
            Supplier<T> action
    ) {
        String redisKey = KEY_PREFIX + userId + ":" + idempotencyKey;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, IN_PROGRESS, TTL);

        if (Boolean.FALSE.equals(acquired)) {

            String stored = redisTemplate.opsForValue().get(redisKey);

            if (stored == null) {
                throw new DuplicateRequestException("Duplicate request detected");
            }

            if (IN_PROGRESS.equals(stored)) {
                throw new DuplicateRequestException("Request already in progress");
            }

            return deserialize(stored, responseType);
        }

        try {
            T result = action.get();
            String serialized = serialize(result);

            redisTemplate.opsForValue().set(redisKey, serialized, TTL);

            return result;

        } catch (Exception ex) {
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }

    private <T> String serialize(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            log.error("Serialization failed", ex);
            throw new DuplicateRequestException("Failed to serialize response");
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            log.error("Deserialization failed", ex);
            throw new DuplicateRequestException("Failed to restore response");
        }
    }
}