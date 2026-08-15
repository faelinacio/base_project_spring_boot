package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.RegisterRequest;
import com.base.project.spring.boot.exception.EmailAlreadyExistsException;
import com.base.project.spring.boot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenIssuer tokenIssuer;

    private RegisterUserUseCase useCase;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordEncoder, tokenIssuer);
    }

    @Test
    void execute_whenEmailNotTaken_savesUserAndIssuesTokens() {
        RegisterRequest request = new RegisterRequest("Rafael", "rafael@example.com", "supersecret123");
        when(userRepository.existsByEmail("rafael@example.com")).thenReturn(false);
        when(passwordEncoder.encode("supersecret123")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 900);
        when(tokenIssuer.issueFor(eq("rafael@example.com"), eq(Role.USER), any(UUID.class))).thenReturn(expected);

        AuthResponse result = useCase.execute(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getName()).isEqualTo("Rafael");
        assertThat(savedUser.getValue().getEmail()).isEqualTo("rafael@example.com");
        assertThat(savedUser.getValue().getPassword()).isEqualTo("encoded-hash");
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getValue().isEnabled()).isTrue();
    }

    @Test
    void execute_whenEmailAlreadyTaken_throwsAndNeverPersists() {
        RegisterRequest request = new RegisterRequest("Rafael", "rafael@example.com", "supersecret123");
        when(userRepository.existsByEmail("rafael@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("rafael@example.com");

        verify(userRepository, never()).save(any());
        verify(tokenIssuer, never()).issueFor(any(), any(), any());
    }

}
