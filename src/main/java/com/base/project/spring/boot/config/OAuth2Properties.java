package com.base.project.spring.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code redirectUri} is the frontend route the user lands on after a Google login, with the issued tokens appended as
 * query params (?accessToken=...&refreshToken=...), or ?error=... on failure.
 */
@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(@NotBlank(message = "app.oauth2.redirect-uri must be set") String redirectUri) {
}
