package com.base.project.spring.boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(

        @NotBlank(message = "token is required") @Schema(description = "Token from the verification link sent by email.") String token) {
}
