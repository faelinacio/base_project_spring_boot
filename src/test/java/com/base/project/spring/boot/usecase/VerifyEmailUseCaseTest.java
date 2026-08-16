package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.base.project.spring.boot.domain.EmailVerificationToken;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.exception.InvalidVerificationTokenException;
import com.base.project.spring.boot.repository.EmailVerificationTokenRepository;
import com.base.project.spring.boot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class VerifyEmailUseCaseTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenHasher hasher;

    private VerifyEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyEmailUseCase(tokenRepository, userRepository, hasher);
    }

    @Test
    void execute_withValidToken_marksUserVerifiedAndConsumesToken() {
        UUID userId = UUID.randomUUID();
        EmailVerificationToken token = EmailVerificationToken.builder().id(UUID.randomUUID()).userId(userId)
                .tokenHash("hashed").used(false).expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(1)).build();
        User user = User.builder().id(userId).emailVerified(false).build();
        when(hasher.hash("raw-token")).thenReturn("hashed");
        when(tokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute("raw-token");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void execute_withUnknownToken_throws() {
        when(hasher.hash("raw-token")).thenReturn("hashed");
        when(tokenRepository.findByTokenHash("hashed")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("raw-token")).isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void execute_withAlreadyUsedToken_throws() {
        EmailVerificationToken token = EmailVerificationToken.builder().userId(UUID.randomUUID()).tokenHash("hashed")
                .used(true).expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(1)).build();
        when(hasher.hash("raw-token")).thenReturn("hashed");
        when(tokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> useCase.execute("raw-token")).isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void execute_withExpiredToken_throws() {
        EmailVerificationToken token = EmailVerificationToken.builder().userId(UUID.randomUUID()).tokenHash("hashed")
                .used(false).expiresAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(1)).build();
        when(hasher.hash("raw-token")).thenReturn("hashed");
        when(tokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> useCase.execute("raw-token")).isInstanceOf(InvalidVerificationTokenException.class);
    }

}
