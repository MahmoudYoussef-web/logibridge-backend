package com.logibridge.backend.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 300; // 5 minutes

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        Attempt attempt = attempts.get(ip);

        if (attempt == null) return true;

        if (attempt.isExpired()) {
            attempts.remove(ip);
            return true;
        }

        return attempt.count < MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        attempts.compute(ip, (key, attempt) -> {
            if (attempt == null || attempt.isExpired()) {
                return new Attempt(1);
            }
            attempt.increment();
            return attempt;
        });
    }

    public void reset(String ip) {
        attempts.remove(ip);
    }

    private static class Attempt {
        int count;
        Instant start;

        Attempt(int count) {
            this.count = count;
            this.start = Instant.now();
        }

        void increment() {
            this.count++;
        }

        boolean isExpired() {
            return Instant.now().isAfter(start.plusSeconds(WINDOW_SECONDS));
        }
    }
}