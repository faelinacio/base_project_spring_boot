package com.base.project.spring.boot.security.jwt;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            @NonNull AuthenticationException authException) throws IOException {
        errorResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                "Authentication is required or the provided token is invalid/expired", request.getRequestURI());
    }

}
