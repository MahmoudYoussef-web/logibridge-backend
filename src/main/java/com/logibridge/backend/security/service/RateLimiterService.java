package com.logibridge.backend.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimiterService {

    private static final int  MAX_ATTEMPTS   = 5;
    private static final long WINDOW_SECONDS = 300; // 5 minutes

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();


    public boolean checkAndRecord(String key) {
        Attempt result = attempts.compute(key, (k, current) -> {
            if (current == null || current.isExpired()) {
                return new Attempt(1);
            }
            current.increment();
            return current;
        });

        boolean withinLimit = result.count <= MAX_ATTEMPTS;

        if (!withinLimit) {
            log.warn("[RATE-LIMIT] key={} blocked — {} failed attempts in {}s window",
                    key, result.count, WINDOW_SECONDS);
        }

        return withinLimit;
    }

    public void reset(String key) {
        attempts.remove(key);
    }



    private static class Attempt {
        int count;
        final Instant windowStart;

        Attempt(int count) {
            this.count       = count;
            this.windowStart = Instant.now();
        }

        void increment() { this.count++; }

        boolean isExpired() {
            return Instant.now().isAfter(windowStart.plusSeconds(WINDOW_SECONDS));
        }
    }
}