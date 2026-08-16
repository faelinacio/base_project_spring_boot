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
import com.base.project.spring.boot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GoogleLoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenIssuer tokenIssuer;

    private GoogleLoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GoogleLoginUseCase(userRepository, tokenIssuer);
    }

    @Test
    void execute_whenNoAccountExists_createsOAuthOnlyAccount() {
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 900);
        when(tokenIssuer.issueFor(org.mockito.ArgumentMatchers.eq("rafael@example.com"),
                org.mockito.ArgumentMatchers.eq(Role.USER), any())).thenReturn(expected);

        AuthResponse result = useCase.execute("google-123", "rafael@example.com", "Rafael");

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getPassword()).isNull();
        assertThat(savedUser.getValue().isEmailVerified()).isTrue();
        assertThat(savedUser.getValue().getGoogleId()).isEqualTo("google-123");
        assertThat(savedUser.getValue().isEnabled()).isTrue();
    }

    @Test
    void execute_whenPasswordAccountWithSameEmailExists_linksGoogleId() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder().id(userId).email("rafael@example.com").password("hash").role(Role.USER)
                .enabled(true).emailVerified(false).build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 900);
        when(tokenIssuer.issueFor("rafael@example.com", Role.USER, userId)).thenReturn(expected);

        AuthResponse result = useCase.execute("google-123", "rafael@example.com", "Rafael");

        assertThat(result).isEqualTo(expected);
        assertThat(existing.getGoogleId()).isEqualTo("google-123");
        assertThat(existing.isEmailVerified()).isTrue();
        assertThat(existing.getPassword()).isEqualTo("hash");
    }

    @Test
    void execute_whenAccountDisabled_throws() {
        UUID userId = UUID.randomUUID();
        User existing = User.builder().id(userId).email("rafael@example.com").googleId("google-123").role(Role.USER)
                .enabled(false).emailVerified(true).build();
        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute("google-123", "rafael@example.com", "Rafael"))
                .isInstanceOf(DisabledException.class);

        verify(tokenIssuer, never()).issueFor(any(), any(), any());
    }

}
