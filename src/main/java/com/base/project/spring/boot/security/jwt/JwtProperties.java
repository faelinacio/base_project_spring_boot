package com.base.project.spring.boot.security.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank(message = "app.jwt.secret must be set (min 256-bit, base64-encoded random value)") String secret,

        @NotNull Duration accessTokenExpiration,

        @NotNull Duration refreshTokenExpiration,

        @NotBlank String issuer) {
}
