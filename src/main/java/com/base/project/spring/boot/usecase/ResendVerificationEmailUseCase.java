package com.base.project.spring.boot.usecase;

import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Always succeeds from the caller's point of view, regardless of whether the account exists or is already verified, so
 * this endpoint can't be used to enumerate registered emails.
 */
@UseCase
@RequiredArgsConstructor
public class ResendVerificationEmailUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationIssuer emailVerificationIssuer;

    @Transactional
    public void execute(String email) {
        userRepository.findByEmail(email).filter(user -> !user.isEmailVerified())
                .ifPresent(emailVerificationIssuer::issueFor);
    }

}
