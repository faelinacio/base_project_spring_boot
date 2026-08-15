package com.base.project.spring.boot.dto;

import java.util.UUID;

import com.base.project.spring.boot.domain.User;

public record UserResponse(UUID id, String name, String email, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

}
