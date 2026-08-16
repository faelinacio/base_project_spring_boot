package com.base.project.spring.boot.usecase;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.exception.InvalidTotpCodeException;
import com.base.project.spring.boot.exception.TotpSetupNotStartedException;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.totp.TotpService;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class EnableTotpUseCase {

    private final UserRepository userRepository;
    private final CurrentUserLoader currentUserLoader;
    private final TotpService totpService;

    @Transactional
    public void execute(UUID userId, String code) {
        User user = currentUserLoader.byId(userId);

        if (user.getTotpSecret() == null) {
            throw new TotpSetupNotStartedException();
        }
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new InvalidTotpCodeException();
        }

        user.setTotpEnabled(true);
        userRepository.save(user);
    }

}
