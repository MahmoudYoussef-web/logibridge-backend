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

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);

        try {

            JwtService.ParsedToken parsed = jwtService.parseAndValidate(token);

            if (!parsed.isAccessToken()) {
                // Refresh tokens hitting protected endpoints = failed auth attempt
                if (rejectAndRecord(ip, response, "INVALID_TOKEN_TYPE")) return;
                filterChain.doFilter(request, response);
                return;
            }

            String email = parsed.username();

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    if (rejectAndRecord(ip, response, "ACCOUNT_DISABLED")) return;
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                rateLimiter.reset(ip);

                logAudit("AUTH_SUCCESS", extractUserId(userDetails),
                        extractRole(userDetails), ip);
            }

        } catch (JwtException ex) {

            SecurityContextHolder.clearContext();
            if (rejectAndRecord(ip, response, "INVALID_TOKEN")) return;

        } catch (UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            if (rejectAndRecord(ip, response, "AUTH_FAILED")) return;

        } catch (Exception ex) {
            log.error("Unexpected error during JWT authentication from ip={}", ip, ex);
            SecurityContextHolder.clearContext();
            if (rejectAndRecord(ip, response, "AUTH_ERROR")) return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean rejectAndRecord(String ip,
                                    HttpServletResponse response,
                                    String auditAction) throws IOException {
        boolean blocked = !rateLimiter.checkAndRecord(ip);
        logAudit(auditAction, null, null, ip);
        if (blocked) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
            return true;
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
            return null;
        }
        return header.substring(7);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails cu) {
            return cu.getId();
        }
        return null;
    }

    private String extractRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("ROLE_UNKNOWN");
    }

    private void logAudit(String action, Long userId, String role, String ip) {
        log.info("[AUDIT] action={} userId={} role={} ip={} timestamp={}",
                action,
                userId != null ? userId : "N/A",
                role   != null ? role   : "N/A",
                ip,
                Instant.now());
    }
}