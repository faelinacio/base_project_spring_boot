package com.base.project.spring.boot.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.base.project.spring.boot.dto.UserResponse;
import com.base.project.spring.boot.security.UserPrincipal;
import com.base.project.spring.boot.usecase.GetCurrentUserUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(getCurrentUserUseCase.execute(principal.getId()));
    }

}
