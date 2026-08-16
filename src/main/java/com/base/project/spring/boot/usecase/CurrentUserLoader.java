package com.base.project.spring.boot.usecase;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Re-loads the account behind an already-authenticated request (by id from a JWT/session principal, or by email from a
 * freshly-checked credential). {@code IllegalStateException} here always means the same thing: authentication just
 * succeeded but the row is gone, an invariant violation rather than a normal error path — never a user-facing 4xx.
 */
@Component
@RequiredArgsConstructor
class CurrentUserLoader {

    private final UserRepository userRepository;

    User byId(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }

    User byEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }

}
