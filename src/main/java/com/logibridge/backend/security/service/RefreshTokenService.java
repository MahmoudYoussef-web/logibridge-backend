package com.logibridge.backend.security.service;

import com.logibridge.backend.auth.entity.RefreshToken;
import com.logibridge.backend.auth.entity.User;
import com.logibridge.backend.auth.repository.RefreshTokenRepository;
import com.logibridge.backend.auth.exception.InvalidTokenException;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;


    @Transactional
    public RefreshToken create(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(generateToken())
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .revoked(false)
                .build();

        return repository.save(token);
    }


    public RefreshToken verify(String token) {
        return repository.findByTokenHash(token)
                .filter(RefreshToken::isValid)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));
    }


    @Transactional
    public RefreshToken rotate(String tokenHash) {

        RefreshToken oldToken = repository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));


        if (oldToken.isRevoked()) {
            handleReuseAttack(oldToken);
            throw new InvalidTokenException("Refresh token reuse detected");
        }

        if (oldToken.isExpired()) {
            throw new InvalidTokenException("Refresh token expired");
        }

        if (oldToken.getReplacedByToken() != null) {
            throw new InvalidTokenException("Token already rotated");
        }

        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now());

        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .tokenHash(generateToken())
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .revoked(false)
                .build();

        oldToken.setReplacedByToken(newToken.getTokenHash());

        repository.save(oldToken);
        return repository.save(newToken);
    }


    @Transactional
    public void deleteAllByUser(User user) {
        repository.deleteAllByUser(user);
    }


    public RefreshToken save(RefreshToken token) {
        return repository.save(token);
    }


    public RefreshToken findByTokenHashOrNull(String tokenHash) {
        return repository.findByTokenHash(tokenHash).orElse(null);
    }


    private void handleReuseAttack(RefreshToken token) {

        repository.deleteAllByUser(token.getUser());
    }


    private String generateToken() {
        return UUID.randomUUID().toString();
    }
}