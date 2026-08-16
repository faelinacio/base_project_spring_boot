package com.base.project.spring.boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken is required") @Schema(description = "Refresh token previously issued by /api/auth/login, /api/auth/login/totp or /api/auth/refresh.") String refreshToken) {
}
