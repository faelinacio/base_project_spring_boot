package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.TotpLoginRequest;
import com.base.project.spring.boot.exception.InvalidMfaTokenException;
import com.base.project.spring.boot.exception.InvalidTotpCodeException;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.jwt.JwtService;
import com.base.project.spring.boot.security.jwt.TokenType;
import com.base.project.spring.boot.security.totp.TotpService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
class VerifyTotpLoginUseCaseTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TotpService totpService;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private Claims claims;

    private VerifyTotpLoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyTotpLoginUseCase(jwtService, userRepository, totpService, tokenIssuer);
    }

    @Test
    void execute_withValidMfaTokenAndCode_issuesTokens() {
        TotpLoginRequest request = new TotpLoginRequest("mfa-token", "123456");
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("rafael@example.com").role(Role.USER).enabled(true)
                .totpEnabled(true).totpSecret("secret").build();
        when(jwtService.parseAndValidate("mfa-token", TokenType.MFA)).thenReturn(Optional.of(claims));
        when(jwtService.extractUsername(claims)).thenReturn("rafael@example.com");
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(user));
        when(totpService.verifyCode("secret", "123456")).thenReturn(true);
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 900);
        when(tokenIssuer.issueFor("rafael@example.com", Role.USER, userId)).thenReturn(expected);

        AuthResponse result = useCase.execute(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void execute_withExpiredMfaToken_throwsInvalidMfaToken() {
        TotpLoginRequest request = new TotpLoginRequest("mfa-token", "123456");
        when(jwtService.parseAndValidate("mfa-token", TokenType.MFA))
                .thenThrow(new ExpiredJwtException(null, Jwts.claims().build(), "expired"));

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(InvalidMfaTokenException.class);
    }

    @Test
    void execute_withNonMfaTokenType_throwsInvalidMfaToken() {
        TotpLoginRequest request = new TotpLoginRequest("access-token", "123456");
        when(jwtService.parseAndValidate("access-token", TokenType.MFA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(InvalidMfaTokenException.class);
    }

    @Test
    void execute_withWrongCode_throwsInvalidTotpCode() {
        TotpLoginRequest request = new TotpLoginRequest("mfa-token", "000000");
        User user = User.builder().id(UUID.randomUUID()).email("rafael@example.com").enabled(true).totpEnabled(true)
                .totpSecret("secret").build();
        when(jwtService.parseAndValidate("mfa-token", TokenType.MFA)).thenReturn(Optional.of(claims));
        when(jwtService.extractUsername(claims)).thenReturn("rafael@example.com");
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(user));
        when(totpService.verifyCode("secret", "000000")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(InvalidTotpCodeException.class);
    }

    @Test
    void execute_whenAccountDisabledBetweenPasswordStepAndTotpStep_throwsInvalidMfaToken() {
        TotpLoginRequest request = new TotpLoginRequest("mfa-token", "123456");
        User user = User.builder().id(UUID.randomUUID()).email("rafael@example.com").enabled(false).totpEnabled(true)
                .totpSecret("secret").build();
        when(jwtService.parseAndValidate("mfa-token", TokenType.MFA)).thenReturn(Optional.of(claims));
        when(jwtService.extractUsername(claims)).thenReturn("rafael@example.com");
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(InvalidMfaTokenException.class)
                .hasMessageContaining("disabled");
    }

}
