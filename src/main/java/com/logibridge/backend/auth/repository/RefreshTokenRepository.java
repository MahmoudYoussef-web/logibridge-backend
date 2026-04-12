package com.logibridge.backend.auth.repository;

import com.logibridge.backend.auth.entity.RefreshToken;
import com.logibridge.backend.auth.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t WHERE t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    // Get all tokens for a user
    List<RefreshToken> findAllByUser(User user);

    // Delete all tokens for user (logout all devices)
    void deleteAllByUser(User user);

    // Delete expired tokens (cleanup job)
    void deleteByExpiresAtBefore(java.time.Instant now);
}