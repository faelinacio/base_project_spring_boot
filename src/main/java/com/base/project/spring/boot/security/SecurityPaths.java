package com.base.project.spring.boot.security;

import java.util.Arrays;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Endpoints that don't require authentication, shared by SecurityConfig and JwtAuthenticationFilter. */
public final class SecurityPaths {

    public static final String[] PUBLIC_ENDPOINTS = { "/api/auth/**", "/error", "/oauth2/**", "/login/oauth2/**" };

    private SecurityPaths() {
    }

    public static RequestMatcher publicEndpointsMatcher() {
        return new OrRequestMatcher(
                Arrays.stream(PUBLIC_ENDPOINTS).map(PathPatternRequestMatcher::pathPattern).toList());
    }

}
