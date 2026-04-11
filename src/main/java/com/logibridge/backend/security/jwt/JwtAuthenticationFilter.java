package com.logibridge.backend.security.jwt;

import com.logibridge.backend.security.service.CustomUserDetails;
import com.logibridge.backend.security.service.CustomUserDetailsService;
import com.logibridge.backend.security.service.RateLimiterService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RateLimiterService rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = extractClientIp(request);
        String token = extractToken(request);


        if (!rateLimiter.isAllowed(ip)) {
            response.setStatus(429);
            response.getWriter().write("Too many requests. Try again later.");
            return;
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtService.isValid(token)) {
                rateLimiter.recordFailure(ip);
                logAudit("AUTH_FAILED", null, null, null, ip);
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.isAccessToken(token)) {
                rateLimiter.recordFailure(ip);
                logAudit("INVALID_TOKEN", null, null, null, ip);
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    rateLimiter.recordFailure(ip);
                    logAudit("AUTH_FAILED", null, null, null, ip);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);


                rateLimiter.reset(ip);

                Long userId = extractUserId(userDetails);
                String role = extractRole(userDetails);

                logAudit("AUTH_SUCCESS", userId, role, null, ip);
            }

        } catch (JwtException ex) {
            rateLimiter.recordFailure(ip);
            logAudit("INVALID_TOKEN", null, null, null, ip);
            SecurityContextHolder.clearContext();

        } catch (UsernameNotFoundException ex) {
            rateLimiter.recordFailure(ip);
            logAudit("AUTH_FAILED", null, null, null, ip);
            SecurityContextHolder.clearContext();

        } catch (Exception ex) {
            rateLimiter.recordFailure(ip);
            log.error("Unexpected authentication error", ex);
            logAudit("AUTH_FAILED", null, null, null, ip);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    // ==================== HELPERS ====================

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUser) {
            return customUser.getId();
        }
        return null;
    }

    private String extractRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("ROLE_UNKNOWN");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            return null;
        }

        return header.substring(7);
    }

    private void logAudit(String action,
                          Long userId,
                          String role,
                          String orderNumber,
                          String ip) {

        log.info("[AUDIT] action={} userId={} role={} order={} ip={} timestamp={}",
                action,
                userId != null ? userId : "N/A",
                role != null ? role : "N/A",
                orderNumber != null ? orderNumber : "-",
                ip,
                Instant.now());
    }
}