package com.logibridge.backend.security.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

@Getter
public final class CustomUserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long                                    id;
    private final String                                  email;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserPrincipal(
            Long id,
            String email,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id          = id;
        this.email       = email;
        this.authorities = Collections.unmodifiableCollection(authorities);
    }

    public static CustomUserPrincipal create(
            Long id,
            String email,
            Collection<? extends GrantedAuthority> authorities
    ) {
        return new CustomUserPrincipal(id, email, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}