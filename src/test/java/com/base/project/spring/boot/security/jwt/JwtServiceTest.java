package com.base.project.spring.boot.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.base.project.spring.boot.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String VALID_SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = newService(VALID_SECRET, Duration.ofMinutes(15), Duration.ofDays(7), "test-issuer");
    }

    private JwtService newService(String secret, Duration accessTtl, Duration refreshTtl, String issuer) {
        JwtService service = new JwtService(new JwtProperties(secret, accessTtl, refreshTtl, issuer));
        service.init();
        return service;
    }

    @Test
    void generateAccessToken_producesAccessTypeToken() {
        JwtService.IssuedToken issued = jwtService.generateAccessToken("rafael@example.com", Role.USER);

        Claims claims = jwtService.parseAndValidate(issued.token());
        assertThat(jwtService.extractTokenType(claims)).isEqualTo(TokenType.ACCESS);
        assertThat(jwtService.extractUsername(claims)).isEqualTo("rafael@example.com");
    }

    @Test
    void generateRefreshToken_producesRefreshTypeToken() {
        JwtService.IssuedToken issued = jwtService.generateRefreshToken("rafael@example.com", Role.USER);

        Claims claims = jwtService.parseAndValidate(issued.token());
        assertThat(jwtService.extractTokenType(claims)).isEqualTo(TokenType.REFRESH);
    }

    @Test
    void parseAndValidate_rejectsTamperedToken() {
        JwtService.IssuedToken issued = jwtService.generateAccessToken("rafael@example.com", Role.USER);
        String tampered = issued.token().substring(0, issued.token().length() - 4) + "abcd";

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsExpiredToken() throws InterruptedException {
        JwtService shortLived = newService(VALID_SECRET, Duration.ofMillis(1), Duration.ofMillis(1), "test-issuer");
        JwtService.IssuedToken issued = shortLived.generateAccessToken("rafael@example.com", Role.USER);

        Thread.sleep(50);

        assertThatThrownBy(() -> shortLived.parseAndValidate(issued.token())).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseAndValidate_rejectsTokenFromDifferentIssuer() {
        JwtService otherIssuer = newService(VALID_SECRET, Duration.ofMinutes(15), Duration.ofDays(7), "other-issuer");
        JwtService.IssuedToken issued = otherIssuer.generateAccessToken("rafael@example.com", Role.USER);

        assertThatThrownBy(() -> jwtService.parseAndValidate(issued.token())).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsTokenSignedWithDifferentSecret() {
        String otherSecret = Base64.getEncoder()
                .encodeToString("99999999999999999999999999999999".getBytes(StandardCharsets.UTF_8));
        JwtService otherKeyService = newService(otherSecret, Duration.ofMinutes(15), Duration.ofDays(7), "test-issuer");
        JwtService.IssuedToken issued = otherKeyService.generateAccessToken("rafael@example.com", Role.USER);

        assertThatThrownBy(() -> jwtService.parseAndValidate(issued.token())).isInstanceOf(JwtException.class);
    }

    @Test
    void init_rejectsSecretShorterThan256Bits() {
        JwtService weak = new JwtService(
                new JwtProperties(Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8)),
                        Duration.ofMinutes(15), Duration.ofDays(7), "test-issuer"));

        assertThatThrownBy(weak::init).isInstanceOf(IllegalStateException.class);
    }

}
