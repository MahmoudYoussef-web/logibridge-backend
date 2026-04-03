package com.logibridge.backend.security.service;

import com.logibridge.backend.auth.entity.User;
import com.logibridge.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {

        String normalizedEmail = email.toLowerCase().trim();

        User user = userRepository.findByEmailWithRoles(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            throw new LockedException("User account is locked");
        }

        return CustomUserDetails.from(user);
    }
}