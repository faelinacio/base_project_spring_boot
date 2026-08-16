package com.base.project.spring.boot.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ResendVerificationEmailUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationIssuer emailVerificationIssuer;

    private ResendVerificationEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResendVerificationEmailUseCase(userRepository, emailVerificationIssuer);
    }

    @Test
    void execute_whenUserExistsAndUnverified_issuesAndSendsNewToken() {
        User user = User.builder().id(UUID.randomUUID()).email("rafael@example.com").emailVerified(false).build();
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationIssuer.issueToken(user)).thenReturn("raw-token");

        useCase.execute("rafael@example.com");

        verify(emailVerificationIssuer).issueToken(user);
        verify(emailVerificationIssuer).sendVerificationEmail(user, "raw-token");
    }

    @Test
    void execute_whenUserAlreadyVerified_doesNothing() {
        User user = User.builder().id(UUID.randomUUID()).email("rafael@example.com").emailVerified(true).build();
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(user));

        useCase.execute("rafael@example.com");

        verify(emailVerificationIssuer, never()).issueToken(any());
        verify(emailVerificationIssuer, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void execute_whenUserDoesNotExist_doesNothing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        useCase.execute("ghost@example.com");

        verify(emailVerificationIssuer, never()).issueToken(any());
    }

}
