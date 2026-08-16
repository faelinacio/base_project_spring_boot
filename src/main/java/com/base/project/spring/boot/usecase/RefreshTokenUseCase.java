package com.base.project.spring.boot.usecase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.RefreshToken;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.exception.InvalidRefreshTokenException;
import com.base.project.spring.boot.repository.RefreshTokenRepository;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.jwt.JwtService;
import com.base.project.spring.boot.security.jwt.TokenType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenHasher hasher;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public AuthResponse execute(String rawRefreshToken) {
        Claims claims = validateAndConsume(rawRefreshToken);

        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token no longer maps to a valid account"));

        if (!user.isEnabled()) {
            throw new InvalidRefreshTokenException("Account is disabled");
        }

        return tokenIssuer.issueFor(user.getEmail(), user.getRole(), user.getId());
    }

    private Claims validateAndConsume(String rawToken) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(rawToken, TokenType.REFRESH)
                    .orElseThrow(() -> new InvalidRefreshTokenException("Token is not a refresh token"));
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidRefreshTokenException("Refresh token is invalid or expired");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hasher.hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is unknown or already used"));

        if (stored.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        refreshTokenRepository.revokeById(stored.getId());
        return claims;
    }

}
