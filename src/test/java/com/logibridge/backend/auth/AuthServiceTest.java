package com.logibridge.backend.auth;

import com.logibridge.backend.auth.dto.RegisterRequest;
import com.logibridge.backend.auth.entity.RefreshToken;
import com.logibridge.backend.auth.entity.Role;
import com.logibridge.backend.auth.entity.User;
import com.logibridge.backend.auth.enums.RoleName;
import com.logibridge.backend.auth.repository.RoleRepository;
import com.logibridge.backend.auth.repository.UserRepository;
import com.logibridge.backend.auth.service.AuthService;
import com.logibridge.backend.common.exception.ConflictException;
import com.logibridge.backend.security.jwt.JwtService;
import com.logibridge.backend.security.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    @Test
    void registerCompany_throws_conflict_when_email_exists() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ahmed")
                .lastName("Ali")
                .email("test@test.com")
                .password("Test1234")
                .build();

        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCompany(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void registerCompany_succeeds_with_valid_request() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ahmed")
                .lastName("Ali")
                .email("new@test.com")
                .password("Test1234")
                .build();

        Role role = Role.builder().name(RoleName.ROLE_COMPANY).build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(roleRepository.findByName(RoleName.ROLE_COMPANY)).thenReturn(Optional.of(role));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(refreshTokenService.create(any())).thenReturn(
                RefreshToken.builder()
                        .tokenHash("refresh-token")
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .revoked(false)
                        .user(new User())
                        .build()
        );

        assertThatCode(() -> authService.registerCompany(request))
                .doesNotThrowAnyException();
    }
}