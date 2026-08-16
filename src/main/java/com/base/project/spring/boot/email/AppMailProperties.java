package com.base.project.spring.boot.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(@NotBlank(message = "app.mail.from-address must be set") String fromAddress) {
}
