//package com.logibridge.backend.security.job;
//
//import com.logibridge.backend.auth.repository.RefreshTokenRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.Instant;
//
//@Component
//@RequiredArgsConstructor
//public class RefreshTokenCleanupJob {
//
//    private final RefreshTokenRepository repository;
//
//    @Scheduled(cron = "0 0 * * * *") // every hour
//    public void cleanupExpiredTokens() {
//        repository.deleteByExpiresAtBefore(Instant.now());
//    }
//}