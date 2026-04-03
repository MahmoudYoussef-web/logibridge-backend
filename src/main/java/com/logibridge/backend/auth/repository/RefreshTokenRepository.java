package com.logibridge.backend.auth.repository;

import com.logibridge.backend.auth.entity.RefreshToken;
import com.logibridge.backend.auth.entity.User;
import org.hibernate.annotations.processing.Find;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {


     // Find token by hash

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Get all tokens for a user

    List<RefreshToken> findAllByUser(User user);

     // Delete all tokens for user (logout all devices)

    void deleteAllByUser(User user);

     // Delete expired tokens (cleanup job)

    void deleteByExpiresAtBefore(java.time.Instant now);
}