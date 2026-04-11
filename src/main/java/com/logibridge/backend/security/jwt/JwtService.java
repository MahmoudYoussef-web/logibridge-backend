package com.logibridge.backend.security.jwt;

import com.logibridge.backend.security.service.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
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



    public String generateToken(CustomUserDetails user) {

        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("uid",  user.getId())
                .claim("type", "ACCESS")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setIssuer("logibridge")
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public ParsedToken parseAndValidate(String token) {
        Claims claims = parse(token).getBody(); // throws JwtException if invalid/expired
        String username    = claims.getSubject();
        String type        = claims.get("type", String.class);
        boolean isAccess   = "ACCESS".equalsIgnoreCase(type);
        return new ParsedToken(username, isAccess);
    }

    public String extractUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parse(token).getBody();
            return claims.getExpiration() != null
                    && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String type = parse(token).getBody().get("type", String.class);
            return "ACCESS".equalsIgnoreCase(type);
        } catch (Exception ex) {
            return false;
        }
    }


    public List<String> extractRoles(String token) {
        return List.of();
    }


    public record ParsedToken(String username, boolean isAccessToken) {}


    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token);
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}