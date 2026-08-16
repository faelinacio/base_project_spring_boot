package com.base.project.spring.boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "email is required") @Email(message = "email must be a valid email address") @Schema(example = "ada@example.com") String email,

        @NotBlank(message = "password is required") @Schema(example = "correct-horse-battery-staple") String password) {
}
