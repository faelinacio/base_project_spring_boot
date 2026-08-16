package com.base.project.spring.boot.usecase;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.EmailVerificationToken;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.exception.InvalidVerificationTokenException;
import com.base.project.spring.boot.repository.EmailVerificationTokenRepository;
import com.base.project.spring.boot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class VerifyEmailUseCase {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final TokenHasher hasher;

    @Transactional
    public void execute(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hasher.hash(rawToken))
                .orElseThrow(() -> new InvalidVerificationTokenException("Verification token is invalid"));

        if (token.isUsed()) {
            throw new InvalidVerificationTokenException("Verification token has already been used");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new InvalidVerificationTokenException("Verification token has expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidVerificationTokenException("Verification token is invalid"));

        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

}
