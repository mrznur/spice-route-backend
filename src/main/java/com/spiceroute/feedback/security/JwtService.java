package com.spiceroute.feedback.security;

import com.spiceroute.feedback.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessMinutes;
    private final long refreshDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-minutes}") long accessMinutes,
                      @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    public String createAccessToken(User user) {
        return build(user, "access", Instant.now().plus(accessMinutes, ChronoUnit.MINUTES));
    }

    public String createRefreshToken(User user) {
        return build(user, "refresh", Instant.now().plus(refreshDays, ChronoUnit.DAYS));
    }

    private String build(User user, String type, Instant expiry) {
        JwtBuilder builder = Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("typ", type)
                .issuedAt(new Date())
                .expiration(Date.from(expiry));
        if (user.getRestaurantId() != null) {
            builder.claim("rid", user.getRestaurantId().toString());
        }
        return builder.signWith(key).compact();
    }

    /** Verifies signature and expiry; throws JwtException if invalid. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /** SHA-256 hex of the raw token string — stored in DB, never the raw JWT. */
    public static String hash(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public long getRefreshDays() { return refreshDays; }
}
