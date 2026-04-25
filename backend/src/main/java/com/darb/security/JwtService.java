package com.darb.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    @Value("${darb.security.jwt.secret}")
    private String jwtSecret;

    @Value("${darb.security.jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;

    @Value("${darb.security.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, "access", accessTokenExpiration);
    }

    public String generateRefreshToken(UUID userId, String email) {
        return buildToken(userId, email, null, "refresh", refreshTokenExpiration);
    }

    private String buildToken(UUID userId, String email, String role, String tokenType, long expiration) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("token_type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(getSigningKey());

        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String getTokenTypeFromToken(String token) {
        return parseToken(token).get("token_type", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT token expired");
        } catch (JwtException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
