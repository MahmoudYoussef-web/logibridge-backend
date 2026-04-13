package com.logibridge.backend.security.job;

import com.logibridge.backend.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository repository;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        int deleted = repository.deleteByExpiresAtBefore(Instant.now());
        log.info("[CLEANUP] Deleted {} expired refresh tokens", deleted);
    }
}