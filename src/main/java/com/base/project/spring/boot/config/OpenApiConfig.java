package com.base.project.spring.boot.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Global OpenAPI metadata and the {@code bearerAuth} scheme referenced by controller {@code @SecurityRequirement}
 * annotations. Served at {@code /v3/api-docs} (JSON) and {@code /swagger-ui.html} (UI), both listed in
 * {@link com.base.project.spring.boot.security.SecurityPaths} so they're reachable without a token.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Base Project Spring Boot API", description = "Authentication, user management and TOTP-based 2FA API for the base Spring Boot project.", version = "v1", contact = @Contact(name = "Base Project Spring Boot"), license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html")))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT access token obtained from /api/auth/login (or /api/auth/login/totp, /api/auth/refresh).")
public class OpenApiConfig {
}
