package com.base.project.spring.boot.security.oauth2;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.base.project.spring.boot.config.OAuth2Properties;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.usecase.GoogleLoginUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Issues our own JWTs for the Google-authenticated user and hands them to the frontend via a redirect, instead of
 * establishing a server-side session — the app stays a stateless JWT API beyond the OAuth2 handshake itself.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleLoginUseCase googleLoginUseCase;
    private final OAuth2Properties oauth2Properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getEmail();

        AuthResponse tokens = googleLoginUseCase.execute(oidcUser.getSubject(), oidcUser.getEmail(), name);

        String redirectUrl = UriComponentsBuilder.fromUriString(oauth2Properties.redirectUri())
                .queryParam("accessToken", tokens.accessToken()).queryParam("refreshToken", tokens.refreshToken())
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

}
