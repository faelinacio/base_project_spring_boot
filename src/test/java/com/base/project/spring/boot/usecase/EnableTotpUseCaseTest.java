package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.exception.InvalidTotpCodeException;
import com.base.project.spring.boot.exception.TotpSetupNotStartedException;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.totp.TotpService;

@ExtendWith(MockitoExtension.class)
class EnableTotpUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserLoader currentUserLoader;

    @Mock
    private TotpService totpService;

    private EnableTotpUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EnableTotpUseCase(userRepository, currentUserLoader, totpService);
    }

    @Test
    void execute_withValidCode_enablesTotp() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).totpSecret("secret").totpEnabled(false).build();
        when(currentUserLoader.byId(userId)).thenReturn(user);
        when(totpService.verifyCode("secret", "123456")).thenReturn(true);

        useCase.execute(userId, "123456");

        assertThat(user.isTotpEnabled()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void execute_withoutPendingSetup_throws() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).totpSecret(null).build();
        when(currentUserLoader.byId(userId)).thenReturn(user);

        assertThatThrownBy(() -> useCase.execute(userId, "123456")).isInstanceOf(TotpSetupNotStartedException.class);
    }

    @Test
    void execute_withInvalidCode_throws() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).totpSecret("secret").totpEnabled(false).build();
        when(currentUserLoader.byId(userId)).thenReturn(user);
        when(totpService.verifyCode("secret", "000000")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(userId, "000000")).isInstanceOf(InvalidTotpCodeException.class);
        assertThat(user.isTotpEnabled()).isFalse();
    }

}
