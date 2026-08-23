package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.exception.EmailNotVerifiedException;
import com.base.project.spring.boot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GoogleLoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginResponseIssuer loginResponseIssuer;

    private GoogleLoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GoogleLoginUseCase(userRepository, loginResponseIssuer);
    }

    @Test
    void execute_whenNoAccountExists_createsOAuthOnlyAccountAndDelegates() {
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        LoginResponse expected = LoginResponse.authenticated(new AuthResponse("access", "refresh", "Bearer", 900));
        when(loginResponseIssuer.issueFor(any(User.class))).thenReturn(expected);

        LoginResponse result = useCase.execute("google-123", "rafael@example.com", "Rafael");

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getPassword()).isNull();
        assertThat(savedUser.getValue().isEmailVerified()).isTrue();
        assertThat(savedUser.getValue().getGoogleId()).isEqualTo("google-123");
        assertThat(savedUser.getValue().isEnabled()).isTrue();
    }

    @Test
    void execute_whenUnverifiedPasswordAccountWithSameEmailExists_linksAndInvalidatesThePassword() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder().id(userId).email("rafael@example.com").password("attacker-controlled-hash")
                .role(Role.USER).enabled(true).emailVerified(false).build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LoginResponse expected = LoginResponse.authenticated(new AuthResponse("access", "refresh", "Bearer", 900));
        when(loginResponseIssuer.issueFor(existing)).thenReturn(expected);

        LoginResponse result = useCase.execute("google-123", "rafael@example.com", "Rafael");

        assertThat(result).isEqualTo(expected);
        assertThat(existing.getGoogleId()).isEqualTo("google-123");
        assertThat(existing.isEmailVerified()).isTrue();
        // The pre-existing password can no longer have been proven to belong to the real owner of this
        // email (nobody had verified it yet), so linking a Google-verified login must invalidate it.
        assertThat(existing.getPassword()).isNull();
    }

    @Test
    void execute_whenVerifiedPasswordAccountWithSameEmailExists_linksAndKeepsThePassword() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder().id(userId).email("rafael@example.com").password("owners-hash").role(Role.USER)
                .enabled(true).emailVerified(true).build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(loginResponseIssuer.issueFor(existing))
                .thenReturn(LoginResponse.authenticated(new AuthResponse("access", "refresh", "Bearer", 900)));

        useCase.execute("google-123", "rafael@example.com", "Rafael");

        assertThat(existing.getPassword()).isEqualTo("owners-hash");
    }

    @Test
    void execute_whenAccountDisabled_throws() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder().id(userId).email("rafael@example.com").googleId("google-123").role(Role.USER)
                .enabled(false).emailVerified(true).build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute("google-123", "rafael@example.com", "Rafael"))
                .isInstanceOf(DisabledException.class);

        verify(loginResponseIssuer, never()).issueFor(any());
    }

    @Test
    void execute_whenReturningLinkedAccountNotEmailVerified_throws() {
        // linkGoogleAccount/createFromGoogle both force emailVerified=true, but a returning
        // already-linked user (googleId already set) skips that block entirely - this proves the
        // use case still re-checks emailVerified afterward instead of trusting a stale value.
        UUID userId = UUID.randomUUID();
        User existing = User.builder().id(userId).email("rafael@example.com").googleId("google-123").role(Role.USER)
                .enabled(true).emailVerified(false).build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute("google-123", "rafael@example.com", "Rafael"))
                .isInstanceOf(EmailNotVerifiedException.class);

        verify(loginResponseIssuer, never()).issueFor(any());
    }

    @Test
    void execute_whenTotpEnabled_returnsMfaRequiredInsteadOfTokens() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder().id(userId).email("rafael@example.com").googleId("google-123").role(Role.USER)
                .enabled(true).emailVerified(true).totpEnabled(true).build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));
        LoginResponse expected = LoginResponse.mfaRequired("mfa-token");
        when(loginResponseIssuer.issueFor(existing)).thenReturn(expected);

        LoginResponse result = useCase.execute("google-123", "rafael@example.com", "Rafael");

        assertThat(result).isEqualTo(expected);
    }

}
