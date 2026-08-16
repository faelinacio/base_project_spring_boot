package com.base.project.spring.boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(String accessToken, String refreshToken, @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Access token lifetime, in seconds.") long expiresIn) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }

}
