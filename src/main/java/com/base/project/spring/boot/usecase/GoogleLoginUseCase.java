package com.base.project.spring.boot.usecase;

import org.springframework.security.authentication.DisabledException;
import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Finds or creates the local account for a successful Google login. Accounts are matched/linked by email: if a
 * password-based account already exists with the same address, the Google identity is linked onto it (safe because
 * CustomOidcUserService already rejected the login upstream unless Google reports the email as verified); otherwise a
 * new OAuth-only account (no password) is created.
 */
@UseCase
@RequiredArgsConstructor
public class GoogleLoginUseCase {

    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public AuthResponse execute(String googleId, String email, String name) {
        User user = userRepository.findByGoogleId(googleId).or(() -> userRepository.findByEmail(email))
                .map(existing -> linkGoogleAccount(existing, googleId))
                .orElseGet(() -> createFromGoogle(googleId, email, name));

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        return tokenIssuer.issueFor(user.getEmail(), user.getRole(), user.getId());
    }

    private User linkGoogleAccount(User user, String googleId) {
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }
        return user;
    }

    private User createFromGoogle(String googleId, String email, String name) {
        User user = User.builder().name(name).email(email).password(null).role(Role.USER).enabled(true)
                .emailVerified(true).googleId(googleId).build();
        return userRepository.save(user);
    }

}
