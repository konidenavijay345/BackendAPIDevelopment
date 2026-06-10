package com.example.backendapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and verifies JSON Web Tokens.
 *
 * <p>The signing key proves a token was issued by this application and was not modified. A short
 * 10-minute lifetime limits damage if a token is exposed while keeping requests stateless.</p>
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;

    /**
     * Creates the service from external configuration and rejects weak signing keys at startup.
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") Duration expiration
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Issues a signed token whose subject is the user's unique email address.
     */
    public String generateToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /** Extracts the authenticated identity after signature and expiry parsing succeeds. */
    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    /** Confirms that the token belongs to the loaded user and has not expired. */
    public boolean isValid(String token, UserDetails user) {
        Claims claims = claims(token);
        return user.getUsername().equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    /** Returns the configured lifetime for API responses and client refresh logic. */
    public long expiresInSeconds() {
        return expiration.toSeconds();
    }

    /** Parses and cryptographically verifies all token claims in one place. */
    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
    }
}
