package com.base.project.spring.boot.usecase;

import java.util.UUID;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public User execute(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }

}
