package com.base.project.spring.boot.security.jwt;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
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

    /** Short-lived token proving password (or OAuth) authentication succeeded, pending the user's TOTP code. */
    public IssuedToken generateMfaToken(String subjectEmail, Role role) {
        return buildToken(subjectEmail, role, TokenType.MFA, properties.mfaTokenExpiration());
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

    /**
     * Same validation as {@link #parseAndValidate(String)}, plus a check that the token is the expected kind (e.g. a
     * REFRESH token can't be used where an MFA token is expected). Still throws JwtException/IllegalArgumentException
     * for a malformed/unsigned/expired token; returns empty (rather than throwing) when the token is otherwise valid
     * but of the wrong {@link TokenType}, since callers each want a different exception/message for that case.
     */
    public Optional<Claims> parseAndValidate(String token, TokenType expectedType) {
        Claims claims = parseAndValidate(token);
        return extractTokenType(claims) == expectedType ? Optional.of(claims) : Optional.empty();
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
