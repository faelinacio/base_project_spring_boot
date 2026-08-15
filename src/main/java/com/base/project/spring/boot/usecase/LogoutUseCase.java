package com.base.project.spring.boot.usecase;

import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher hasher;

    @Transactional
    public void execute(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hasher.hash(rawRefreshToken))
                .ifPresent(rt -> refreshTokenRepository.revokeById(rt.getId()));
    }

}
