package com.logibridge.backend.security.jwt;

import com.logibridge.backend.security.service.CustomUserDetailsService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                var userDetails = userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (Exception ex) {
            log.warn("JWT authentication failed");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            return null;
        }

        return header.substring(7);
    }
}