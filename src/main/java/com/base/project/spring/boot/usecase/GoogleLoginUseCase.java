package com.base.project.spring.boot.usecase;

import org.springframework.security.authentication.DisabledException;
import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * Finds or creates the local account for a successful Google login. Accounts are matched/linked by email: if a
 * password-based account already exists with the same address, the Google identity is linked onto it; otherwise a new
 * OAuth-only account (no password) is created. Mirrors LoginUseCase's TOTP branch so 2FA can't be bypassed by signing
 * in with Google instead of a password.
 */
@UseCase
@RequiredArgsConstructor
public class GoogleLoginUseCase {

    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse execute(String googleId, String email, String name) {
        User user = userRepository.findByGoogleId(googleId).or(() -> userRepository.findByEmail(email))
                .map(existing -> linkGoogleAccount(existing, googleId))
                .orElseGet(() -> createFromGoogle(googleId, email, name));

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (user.isTotpEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user.getEmail(), user.getRole()).token();
            return LoginResponse.mfaRequired(mfaToken);
        }

        return LoginResponse.authenticated(tokenIssuer.issueFor(user.getEmail(), user.getRole(), user.getId()));
    }

    private User linkGoogleAccount(User user, String googleId) {
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            if (!user.isEmailVerified()) {
                // This account's email was never proven to belong to whoever set its password (e.g. an
                // attacker could have pre-registered this address hoping to hijack it once the real
                // owner signs in with Google). Google's verified login takes precedence and invalidates
                // that password so it can no longer be used to authenticate.
                user.setPassword(null);
            }
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
