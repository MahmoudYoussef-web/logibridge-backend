package com.logibridge.backend.auth.service;

import com.logibridge.backend.auth.dto.*;
import com.logibridge.backend.auth.entity.*;
import com.logibridge.backend.auth.enums.RoleName;
import com.logibridge.backend.auth.enums.UserStatus;
import com.logibridge.backend.auth.mapper.AuthMapper;
import com.logibridge.backend.auth.repository.*;
import com.logibridge.backend.common.exception.*;
import com.logibridge.backend.security.jwt.JwtService;
import com.logibridge.backend.security.service.CustomUserDetails;
import com.logibridge.backend.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;


    public AuthResponse registerCompany(RegisterRequest request) {
        return register(request, RoleName.ROLE_COMPANY);
    }

    public AuthResponse registerDelivery(RegisterRequest request) {
        return register(request, RoleName.ROLE_DELIVERY);
    }

    private AuthResponse register(RegisterRequest request, RoleName roleName) {

        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed — email already in use: {}", email);
            throw new ConflictException("Email already in use");
        }

        User user = AuthMapper.toUser(request, passwordEncoder.encode(request.getPassword()));


        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        user.addRole(role);
        userRepository.save(user);

        log.info("User registered: email={}, role={}", email, roleName);

        return generateAuthResponse(user);
    }


    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();


            if (userDetails.getUser().getStatus() != UserStatus.ACTIVE) {
                log.warn("Login blocked — user not active: {}", email);
                throw new UnauthorizedException("Account not active");
            }

            log.info("User logged in: email={}", email);

            return generateAuthResponse(userDetails.getUser());

        } catch (AuthenticationException ex) {
            log.warn("Login failed for email={}: {}", email, ex.getMessage());
            throw new UnauthorizedException("Invalid email or password");
        }
    }


    public AuthResponse refresh(RefreshTokenRequest request) {

        RefreshToken newToken = refreshTokenService.rotate(request.getRefreshToken());
        String accessToken = jwtService.generateToken(CustomUserDetails.from(newToken.getUser()));

        return AuthResponse.of(accessToken, newToken.getTokenHash());
    }


    public void logout(String refreshToken) {

        RefreshToken token = refreshTokenService.findByTokenHashOrNull(refreshToken);

        if (token == null || token.isRevoked()) return;

        token.setRevoked(true);
        token.setRevokedAt(Instant.now());

        refreshTokenService.save(token);
    }

    public void logoutAll(String email) {

        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenService.deleteAllByUser(user);
    }


    private AuthResponse generateAuthResponse(User user) {
        String accessToken   = jwtService.generateToken(CustomUserDetails.from(user));
        RefreshToken refresh = refreshTokenService.create(user);
        return AuthResponse.of(accessToken, refresh.getTokenHash());
    }
}