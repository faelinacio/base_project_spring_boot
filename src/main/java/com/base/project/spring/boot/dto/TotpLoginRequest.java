package com.base.project.spring.boot.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpLoginRequest(

        @NotBlank(message = "mfaToken is required") String mfaToken,

        @NotBlank(message = "code is required") String code) {
}
