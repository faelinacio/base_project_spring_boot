package com.base.project.spring.boot.usecase;

import java.util.UUID;

import com.base.project.spring.boot.domain.User;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final CurrentUserLoader currentUserLoader;

    public User execute(UUID userId) {
        return currentUserLoader.byId(userId);
    }

}
