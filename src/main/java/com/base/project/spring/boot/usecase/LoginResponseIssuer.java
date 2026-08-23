package com.base.project.spring.boot.usecase;

import org.springframework.stereotype.Component;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * Shared by {@link LoginUseCase} and {@link GoogleLoginUseCase}: once an identity is resolved and its
 * enabled/emailVerified pre-checks pass, this decides the last step identically for both - full tokens, or an mfaToken
 * challenge - so the two login paths can't drift on how TOTP is enforced.
 */
@Component
@RequiredArgsConstructor
class LoginResponseIssuer {

    private final TokenIssuer tokenIssuer;
    private final JwtService jwtService;

    LoginResponse issueFor(User user) {
        if (user.isTotpEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user.getEmail(), user.getRole()).token();
            return LoginResponse.mfaRequired(mfaToken);
        }

        return LoginResponse.authenticated(tokenIssuer.issueFor(user.getEmail(), user.getRole(), user.getId()));
    }

}
