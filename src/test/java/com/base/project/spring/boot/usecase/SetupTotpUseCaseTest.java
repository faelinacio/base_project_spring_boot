package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.base.project.spring.boot.dto.TotpSetupResponse;
import com.base.project.spring.boot.exception.TotpAlreadyEnabledException;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.totp.TotpService;

@ExtendWith(MockitoExtension.class)
class SetupTotpUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TotpService totpService;

    private SetupTotpUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SetupTotpUseCase(userRepository, totpService);
    }

    @Test
    void execute_whenTotpNotYetEnabled_generatesAndStoresSecret() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("rafael@example.com").totpEnabled(false).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpService.generateSecret()).thenReturn("secret");
        when(totpService.generateQrCodeDataUri("secret", "rafael@example.com")).thenReturn("data:image/png;base64,x");

        TotpSetupResponse response = useCase.execute(userId);

        assertThat(response.secret()).isEqualTo("secret");
        assertThat(user.getTotpSecret()).isEqualTo("secret");
        assertThat(user.isTotpEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void execute_whenTotpAlreadyEnabled_throwsAndNeverOverwritesSecret() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("rafael@example.com").totpEnabled(true)
                .totpSecret("existing-secret").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute(userId)).isInstanceOf(TotpAlreadyEnabledException.class);

        assertThat(user.getTotpSecret()).isEqualTo("existing-secret");
        verify(userRepository, never()).save(user);
    }

}
