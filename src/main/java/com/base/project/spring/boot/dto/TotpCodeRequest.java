package com.base.project.spring.boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TotpCodeRequest(

        @NotBlank(message = "code is required") @Schema(example = "123456", description = "6-digit code from the user's authenticator app.") String code) {
}
