package com.base.project.spring.boot.usecase;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.LoginRequest;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.exception.EmailNotVerifiedException;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final JwtService jwtService;

    public LoginResponse execute(LoginRequest request) {
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(user.getEmail());
        }

        if (user.isTotpEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user.getEmail(), user.getRole()).token();
            return LoginResponse.mfaRequired(mfaToken);
        }

        return LoginResponse.authenticated(tokenIssuer.issueFor(user.getEmail(), user.getRole(), user.getId()));
    }

}
