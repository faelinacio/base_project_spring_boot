package com.base.project.spring.boot.security.jwt;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Registered as the SecurityFilterChain's AccessDeniedHandler for any future URL-pattern-based authorization rule.
 * Today the app's only real 403 (AdminController's @PreAuthorize) is resolved by GlobalExceptionHandler inside Spring
 * MVC dispatch before it reaches this class, so this handler stays dormant until a rule is added to
 * authorizeHttpRequests() — kept for that case, sharing its JSON-writing logic with JwtAuthenticationEntryPoint via
 * SecurityErrorResponseWriter.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        errorResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                "You do not have permission to access this resource", request.getRequestURI());
    }

}
