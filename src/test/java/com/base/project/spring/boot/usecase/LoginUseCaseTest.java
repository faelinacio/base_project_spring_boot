package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.LoginRequest;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.exception.EmailNotVerifiedException;
import com.base.project.spring.boot.security.jwt.JwtService;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CurrentUserLoader currentUserLoader;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private JwtService jwtService;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(authenticationManager, currentUserLoader, tokenIssuer, jwtService);
    }

    @Test
    void execute_whenCredentialsValidAndEmailVerified_authenticatesAndIssuesTokens() {
        LoginRequest request = new LoginRequest("rafael@example.com", "supersecret123");
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("rafael@example.com").role(Role.USER).enabled(true)
                .emailVerified(true).build();
        when(currentUserLoader.byEmail("rafael@example.com")).thenReturn(user);
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 900);
        when(tokenIssuer.issueFor("rafael@example.com", Role.USER, userId)).thenReturn(expected);

        LoginResponse result = useCase.execute(request);

        assertThat(result).isEqualTo(LoginResponse.authenticated(expected));
        verify(authenticationManager)
                .authenticate(eq(new UsernamePasswordAuthenticationToken("rafael@example.com", "supersecret123")));
    }

    @Test
    void execute_whenAuthenticationFails_propagatesException() {
        LoginRequest request = new LoginRequest("rafael@example.com", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void execute_whenUserVanishesAfterAuthentication_propagatesCurrentUserLoaderException() {
        LoginRequest request = new LoginRequest("rafael@example.com", "supersecret123");
        when(currentUserLoader.byEmail("rafael@example.com"))
                .thenThrow(new IllegalStateException("Authenticated user no longer exists"));

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void execute_whenEmailNotVerified_throwsAndNeverIssuesTokens() {
        LoginRequest request = new LoginRequest("rafael@example.com", "supersecret123");
        User user = User.builder().id(UUID.randomUUID()).email("rafael@example.com").role(Role.USER).enabled(true)
                .emailVerified(false).build();
        when(currentUserLoader.byEmail("rafael@example.com")).thenReturn(user);

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(EmailNotVerifiedException.class);

        verify(tokenIssuer, never()).issueFor(any(), any(), any());
    }

    @Test
    void execute_whenTotpEnabled_returnsMfaRequiredInsteadOfTokens() {
        LoginRequest request = new LoginRequest("rafael@example.com", "supersecret123");
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("rafael@example.com").role(Role.USER).enabled(true)
                .emailVerified(true).totpEnabled(true).build();
        when(currentUserLoader.byEmail("rafael@example.com")).thenReturn(user);
        JwtService.IssuedToken mfaToken = new JwtService.IssuedToken("mfa-token", null);
        when(jwtService.generateMfaToken("rafael@example.com", Role.USER)).thenReturn(mfaToken);

        LoginResponse result = useCase.execute(request);

        assertThat(result.mfaRequired()).isTrue();
        assertThat(result.mfaToken()).isEqualTo("mfa-token");
        assertThat(result.tokens()).isNull();
        verify(tokenIssuer, never()).issueFor(any(), any(), any());
    }

}
