package com.base.project.spring.boot.security.oauth2;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.base.project.spring.boot.config.OAuth2Properties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuth2Properties oauth2Properties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        // Deliberately generic: never forward the provider's raw error/exception message to the browser.
        String redirectUrl = UriComponentsBuilder.fromUriString(oauth2Properties.redirectUri())
                .queryParam("error", "oauth2_login_failed").build().toUriString();

        response.sendRedirect(redirectUrl);
    }

}
