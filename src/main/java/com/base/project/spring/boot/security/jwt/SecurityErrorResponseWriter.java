package com.base.project.spring.boot.security.jwt;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.base.project.spring.boot.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes a JSON {@link ErrorResponse} directly to the servlet response, for the two security entry points
 * ({@code JwtAuthenticationEntryPoint}, {@code JwtAccessDeniedHandler}) that run outside Spring MVC dispatch and so
 * can't go through {@code GlobalExceptionHandler}.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, String error, String message, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(status, error, message, path);
        objectMapper.writeValue(response.getWriter(), body);
    }

}
