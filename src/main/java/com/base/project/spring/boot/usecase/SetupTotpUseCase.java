package com.base.project.spring.boot.usecase;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.TotpSetupResponse;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.totp.TotpService;

import lombok.RequiredArgsConstructor;

/**
 * Generates a fresh TOTP secret for the user and stores it unconfirmed; 2FA only takes effect once EnableTotpUseCase
 * verifies a code against it. Calling this again before enabling replaces the pending secret.
 */
@UseCase
@RequiredArgsConstructor
public class SetupTotpUseCase {

    private final UserRepository userRepository;
    private final TotpService totpService;

    @Transactional
    public TotpSetupResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpEnabled(false);
        userRepository.save(user);

        return new TotpSetupResponse(secret, totpService.generateQrCodeDataUri(secret, user.getEmail()));
    }

}
