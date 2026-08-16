package com.base.project.spring.boot.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.RegisterRequest;
import com.base.project.spring.boot.exception.EmailAlreadyExistsException;
import com.base.project.spring.boot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final EmailVerificationIssuer emailVerificationIssuer;

    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // emailVerified defaults to false: password-based accounts must confirm their email (via the
        // link EmailVerificationIssuer sends below) before they can log in again.
        User user = User.builder().name(request.name()).email(request.email())
                .password(passwordEncoder.encode(request.password())).role(Role.USER).enabled(true).build();
        User saved = userRepository.save(user);

        emailVerificationIssuer.issueFor(saved);

        return tokenIssuer.issueFor(saved.getEmail(), saved.getRole(), saved.getId());
    }

}
