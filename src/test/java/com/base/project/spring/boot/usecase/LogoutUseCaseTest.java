package com.base.project.spring.boot.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.base.project.spring.boot.domain.RefreshToken;
import com.base.project.spring.boot.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final TokenHasher hasher = new TokenHasher();

    private LogoutUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCase(refreshTokenRepository, hasher);
    }

    @Test
    void execute_whenTokenExists_revokesIt() {
        String rawToken = "some-refresh-token";
        UUID id = UUID.randomUUID();
        RefreshToken stored = RefreshToken.builder().id(id).build();
        when(refreshTokenRepository.findByTokenHash(hasher.hash(rawToken))).thenReturn(Optional.of(stored));

        useCase.execute(rawToken);

        verify(refreshTokenRepository).revokeById(id);
    }

    @Test
    void execute_whenTokenUnknown_doesNothingSilently() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        useCase.execute("unknown-token");

        verify(refreshTokenRepository, never()).revokeById(any());
    }

}
