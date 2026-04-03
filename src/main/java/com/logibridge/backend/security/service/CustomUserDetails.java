package com.logibridge.backend.security.service;

import com.logibridge.backend.auth.entity.User;
import com.logibridge.backend.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private boolean accountNonLocked;
    private boolean enabled;
    private Collection<? extends GrantedAuthority> authorities;

    private User user;

    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isAccountNonLocked(),
                user.isEnabled(),
                user.getUserRoles().stream()
                        .map(UserRole::getRole)
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .toList(),
                user
        );
    }

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}