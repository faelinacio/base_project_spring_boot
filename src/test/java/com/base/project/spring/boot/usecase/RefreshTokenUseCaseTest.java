package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.domain.RefreshToken;
import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.exception.InvalidRefreshTokenException;
import com.base.project.spring.boot.repository.RefreshTokenRepository;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.jwt.JwtService;
import com.base.project.spring.boot.security.jwt.TokenType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;

/**
 * JwtService is mocked here (not exercised with real crypto) so this test stays a pure unit test of the
 * rotation/validation business rules; JwtService's own crypto correctness is covered by JwtServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String EMAIL = "rafael@example.com";

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenIssuer tokenIssuer;

    private final TokenHasher hasher = new TokenHasher();

    private RefreshTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(jwtService, refreshTokenRepository, userRepository, hasher, tokenIssuer);
    }

    private Claims claimsWithType(String type) {
        return Jwts.claims().subject(EMAIL).add("type", type).build();
    }

    private RefreshToken storedToken(UUID userId, boolean revoked, LocalDateTime expiresAt) {
        return RefreshToken.builder().id(UUID.randomUUID()).userId(userId).tokenHash(hasher.hash(RAW_TOKEN))
                .revoked(revoked).expiresAt(expiresAt).build();
    }

    @Test
    void execute_whenTokenValid_rotatesAndReturnsNewTokens() {
        Claims claims = claimsWithType("REFRESH");
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenReturn(claims);
        when(jwtService.extractTokenType(claims)).thenReturn(TokenType.REFRESH);

        UUID userId = UUID.randomUUID();
        RefreshToken stored = storedToken(userId, false, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(refreshTokenRepository.findByTokenHash(hasher.hash(RAW_TOKEN))).thenReturn(Optional.of(stored));

        User user = User.builder().id(userId).email(EMAIL).role(Role.USER).enabled(true).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        AuthResponse expected = new AuthResponse("new-access", "new-refresh", "Bearer", 900);
        when(tokenIssuer.issueFor(EMAIL, Role.USER, userId)).thenReturn(expected);

        AuthResponse result = useCase.execute(RAW_TOKEN);

        assertThat(result).isEqualTo(expected);
        verify(refreshTokenRepository).revokeById(stored.getId());
    }

    @Test
    void execute_whenUnderlyingJwtInvalid_throwsInvalidRefreshTokenException() {
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenThrow(new SignatureException("bad signature"));

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN)).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("invalid or expired");
    }

    @Test
    void execute_whenTokenIsAnAccessToken_throwsInvalidRefreshTokenException() {
        Claims claims = claimsWithType("ACCESS");
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenReturn(claims);
        when(jwtService.extractTokenType(claims)).thenReturn(TokenType.ACCESS);

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN)).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("not a refresh token");
    }

    @Test
    void execute_whenTokenUnknown_throwsInvalidRefreshTokenException() {
        Claims claims = claimsWithType("REFRESH");
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenReturn(claims);
        when(jwtService.extractTokenType(claims)).thenReturn(TokenType.REFRESH);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN)).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("unknown or already used");
    }

    @Test
    void execute_whenTokenAlreadyRevoked_throwsInvalidRefreshTokenException() {
        Claims claims = claimsWithType("REFRESH");
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenReturn(claims);
        when(jwtService.extractTokenType(claims)).thenReturn(TokenType.REFRESH);
        RefreshToken stored = storedToken(UUID.randomUUID(), true, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(refreshTokenRepository.findByTokenHash(hasher.hash(RAW_TOKEN))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN)).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void execute_whenTokenExpired_throwsInvalidRefreshTokenException() {
        Claims claims = claimsWithType("REFRESH");
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenReturn(claims);
        when(jwtService.extractTokenType(claims)).thenReturn(TokenType.REFRESH);
        RefreshToken stored = storedToken(UUID.randomUUID(), false, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(refreshTokenRepository.findByTokenHash(hasher.hash(RAW_TOKEN))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN)).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void execute_whenUserNoLongerExists_throwsInvalidRefreshTokenException() {
        Claims claims = claimsWithType("REFRESH");
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenReturn(claims);
        when(jwtService.extractTokenType(claims)).thenReturn(TokenType.REFRESH);
        RefreshToken stored = storedToken(UUID.randomUUID(), false, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(refreshTokenRepository.findByTokenHash(hasher.hash(RAW_TOKEN))).thenReturn(Optional.of(stored));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN)).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("no longer maps to a valid account");
    }

    @Test
    void execute_whenAccountDisabled_throwsInvalidRefreshTokenException_andConsumesToken() {
        Claims claims = claimsWithType("REFRESH");
        when(jwtService.parseAndValidate(RAW_TOKEN)).thenReturn(claims);
        when(jwtService.extractTokenType(claims)).thenReturn(TokenType.REFRESH);
        UUID userId = UUID.randomUUID();
        RefreshToken stored = storedToken(userId, false, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(refreshTokenRepository.findByTokenHash(hasher.hash(RAW_TOKEN))).thenReturn(Optional.of(stored));
        User disabledUser = User.builder().id(userId).email(EMAIL).role(Role.USER).enabled(false).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> useCase.execute(RAW_TOKEN)).isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("disabled");

        // the token was already revoked as part of validation, even though issuance was refused
        verify(refreshTokenRepository).revokeById(stored.getId());
        verify(tokenIssuer, never()).issueFor(anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

}
