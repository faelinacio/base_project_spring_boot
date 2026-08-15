package com.base.project.spring.boot.usecase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.base.project.spring.boot.domain.RefreshToken;
import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.repository.RefreshTokenRepository;
import com.base.project.spring.boot.security.jwt.JwtProperties;
import com.base.project.spring.boot.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * Shared by the register/login/refresh use cases: issues an access+refresh token pair for an identity and persists the
 * refresh token (hashed) so it can later be revoked or rotated.
 */
@Component
@RequiredArgsConstructor
class TokenIssuer {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher hasher;
    private final JwtProperties jwtProperties;

    AuthResponse issueFor(String email, Role role, UUID userId) {
        JwtService.IssuedToken access = jwtService.generateAccessToken(email, role);
        JwtService.IssuedToken refresh = jwtService.generateRefreshToken(email, role);

        refreshTokenRepository.save(RefreshToken.builder().userId(userId).tokenHash(hasher.hash(refresh.token()))
                .expiresAt(LocalDateTime.ofInstant(refresh.expiresAt(), ZoneOffset.UTC)).revoked(false).build());

        return AuthResponse.of(access.token(), refresh.token(), jwtProperties.accessTokenExpiration().toSeconds());
    }

}
