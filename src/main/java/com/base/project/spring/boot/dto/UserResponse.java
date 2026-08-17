package com.base.project.spring.boot.dto;

import java.util.UUID;

import com.base.project.spring.boot.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(UUID id, String name, String email, @Schema(example = "USER") String role,
        boolean totpEnabled) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                user.isTotpEnabled());
    }

}
