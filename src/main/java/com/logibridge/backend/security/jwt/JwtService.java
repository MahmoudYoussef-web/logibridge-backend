package com.logibridge.backend.security.jwt;

import com.logibridge.backend.security.service.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${auth.jwt.secret}")
    private String secret;

    @Value("${auth.jwt.expiration}")
    private long expiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(CustomUserDetails user) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", roles)
                .claim("type", "ACCESS")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setIssuer("logibridge")
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getBody().getSubject();
    }


    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Object rolesClaim = parse(token).getBody().get("roles");
            if (rolesClaim instanceof List<?> list) {
                return list.stream()
                        .filter(item -> item instanceof String)
                        .map(item -> (String) item)
                        .toList();
            }
        } catch (Exception ex) {
            // intentionally silent — caller handles empty list
        }
        return List.of();
    }

    public boolean isAccessToken(String token) {
        try {
            String type = parse(token).getBody().get("type", String.class);
            return "ACCESS".equalsIgnoreCase(type);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parse(token).getBody();
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token);
    }
}