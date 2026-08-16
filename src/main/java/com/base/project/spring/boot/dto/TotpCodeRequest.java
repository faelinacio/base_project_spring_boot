package com.base.project.spring.boot.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpCodeRequest(

        @NotBlank(message = "code is required") String code) {
}
