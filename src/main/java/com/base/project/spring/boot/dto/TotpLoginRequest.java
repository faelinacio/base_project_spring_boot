package com.base.project.spring.boot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TotpLoginRequest(

        @NotBlank(message = "mfaToken is required") @Schema(description = "mfaToken returned by /api/auth/login when the account has TOTP enabled.") String mfaToken,

        @NotBlank(message = "code is required") @Schema(example = "123456", description = "6-digit code from the user's authenticator app.") String code) {
}
