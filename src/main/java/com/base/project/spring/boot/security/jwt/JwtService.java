package com.base.project.spring.boot.security.jwt;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.base.project.spring.boot.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;

/**
 * Issues and validates signed JWTs. Access tokens are short-lived and stateless; refresh tokens are long-lived and
 * additionally tracked (hashed) in the database by {@code RefreshTokenService} so they can be revoked/rotated.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private Key signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = Base64.getDecoder().decode(properties.secret());
        try {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            throw new IllegalStateException(
                    "app.jwt.secret is too weak: it must decode to at least 256 bits (32 bytes) for HS256", e);
        }
    }

    public IssuedToken generateAccessToken(String subjectEmail, Role role) {
        return buildToken(subjectEmail, role, TokenType.ACCESS, properties.accessTokenExpiration());
    }

    public IssuedToken generateRefreshToken(String subjectEmail, Role role) {
        return buildToken(subjectEmail, role, TokenType.REFRESH, properties.refreshTokenExpiration());
    }

    private IssuedToken buildToken(String subjectEmail, Role role, TokenType type, Duration expiration) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        String token = Jwts.builder().id(UUID.randomUUID().toString()).issuer(properties.issuer()).subject(subjectEmail)
                .claim(CLAIM_TYPE, type.name()).claim(CLAIM_ROLE, role.name()).issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt)).signWith((SecretKey) signingKey).compact();

        return new IssuedToken(token, expiresAt);
    }

    /** Validates signature, issuer and expiration; throws JwtException/IllegalArgumentException otherwise. */
    public Claims parseAndValidate(String token) {
        return Jwts.parser().verifyWith((SecretKey) signingKey).requireIssuer(properties.issuer()).build()
                .parseSignedClaims(token).getPayload();
    }

    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }

    public TokenType extractTokenType(Claims claims) {
        return TokenType.valueOf(claims.get(CLAIM_TYPE, String.class));
    }

    /** A freshly issued JWT together with the expiration instant already computed for it. */
    public record IssuedToken(String token, Instant expiresAt) {
    }

}
