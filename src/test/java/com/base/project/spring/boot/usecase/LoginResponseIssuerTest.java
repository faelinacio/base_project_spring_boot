package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.security.jwt.JwtService;

@ExtendWith(MockitoExtension.class)
class LoginResponseIssuerTest {

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private JwtService jwtService;

    private LoginResponseIssuer issuer;

    @BeforeEach
    void setUp() {
        issuer = new LoginResponseIssuer(tokenIssuer, jwtService);
    }

    @Test
    void issueFor_whenTotpDisabled_issuesFullTokens() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("rafael@example.com").role(Role.USER).build();
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 900);
        when(tokenIssuer.issueFor("rafael@example.com", Role.USER, userId)).thenReturn(expected);

        LoginResponse result = issuer.issueFor(user);

        assertThat(result.mfaRequired()).isFalse();
        assertThat(result.tokens()).isEqualTo(expected);
        verify(jwtService, never()).generateMfaToken(any(), any());
    }

    @Test
    void issueFor_whenTotpEnabled_returnsMfaRequiredInsteadOfTokens() {
        User user = User.builder().id(UUID.randomUUID()).email("rafael@example.com").role(Role.USER).totpEnabled(true)
                .build();
        JwtService.IssuedToken mfaToken = new JwtService.IssuedToken("mfa-token", null);
        when(jwtService.generateMfaToken("rafael@example.com", Role.USER)).thenReturn(mfaToken);

        LoginResponse result = issuer.issueFor(user);

        assertThat(result.mfaRequired()).isTrue();
        assertThat(result.mfaToken()).isEqualTo("mfa-token");
        assertThat(result.tokens()).isNull();
        verify(tokenIssuer, never()).issueFor(any(), any(), any());
    }

}
