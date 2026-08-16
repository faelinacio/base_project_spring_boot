package com.base.project.spring.boot.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(

        @NotBlank(message = "app.email-verification.verification-base-url must be set") String verificationBaseUrl,

        @NotNull Duration tokenExpiration) {
}
