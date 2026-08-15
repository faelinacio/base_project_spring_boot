package com.base.project.spring.boot.security.jwt;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.base.project.spring.boot.security.SecurityPaths;
import com.base.project.spring.boot.security.UserPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Reads the {@code Authorization: Bearer <token>} header, validates it as an access token, and — if valid — populates
 * the SecurityContext so downstream authorization checks (authorizeHttpRequests / @PreAuthorize) can run. Never throws
 * on a missing/invalid token: it just leaves the request unauthenticated and lets the filter chain's access rules + the
 * AuthenticationEntryPoint produce the 401.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RequestMatcher permitAllMatcher = SecurityPaths.publicEndpointsMatcher();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return permitAllMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtService.parseAndValidate(token);

                if (jwtService.extractTokenType(claims) == TokenType.ACCESS) {
                    String email = jwtService.extractUsername(claims);
                    UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(email);

                    var authToken = new UsernamePasswordAuthenticationToken(principal, null,
                            principal.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Invalid/expired token, or the token's subject no longer maps to a user: leave the
            // request unauthenticated and let JwtAuthenticationEntryPoint produce the 401.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

}
