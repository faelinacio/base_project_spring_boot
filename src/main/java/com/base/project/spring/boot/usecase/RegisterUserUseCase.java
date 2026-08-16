package com.base.project.spring.boot.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;

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

    // Not @Transactional: existsByEmail-then-save is a check-then-act that GlobalExceptionHandler's
    // DataIntegrityViolationException handler already accounts for (a wider transaction wouldn't close
    // that race either), and keeping each step in its own short transaction means the token write below
    // commits before the (potentially slow) verification email is sent, instead of holding a DB
    // connection open for the whole SMTP round trip.
    public AuthResponse execute(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // emailVerified defaults to false: password-based accounts must confirm their email (via the
        // link sent below) before they can log in again.
        User user = User.builder().name(request.name()).email(request.email())
                .password(passwordEncoder.encode(request.password())).role(Role.USER).enabled(true).build();
        User saved = userRepository.save(user);

        String verificationToken = emailVerificationIssuer.issueToken(saved);
        emailVerificationIssuer.sendVerificationEmail(saved, verificationToken);

        return tokenIssuer.issueFor(saved.getEmail(), saved.getRole(), saved.getId());
    }

}
