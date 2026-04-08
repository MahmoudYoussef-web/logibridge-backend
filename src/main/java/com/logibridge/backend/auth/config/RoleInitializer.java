package com.logibridge.backend.auth.config;

import com.logibridge.backend.auth.entity.Role;
import com.logibridge.backend.auth.enums.RoleName;
import com.logibridge.backend.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;

@Configuration
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;

    @Bean
    public CommandLineRunner initRoles() {
        return args -> {
            for (RoleName roleName : RoleName.values()) {
                roleRepository.findByName(roleName)
                        .orElseGet(() -> {
                            try {
                                return roleRepository.save(
                                        Role.builder()
                                                .name(roleName)
                                                .build()
                                );
                            } catch (DataIntegrityViolationException ex) {
                                return roleRepository.findByName(roleName).orElse(null);
                            }
                        });
            }
        };
    }
}