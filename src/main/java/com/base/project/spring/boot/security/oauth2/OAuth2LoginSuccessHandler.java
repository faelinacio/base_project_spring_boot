package com.base.project.spring.boot.security.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.base.project.spring.boot.config.OAuth2Properties;
import com.base.project.spring.boot.dto.LoginResponse;
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
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final OAuth2Properties oauth2Properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getEmail();

        LoginResponse result;
        try {
            result = googleLoginUseCase.execute(oidcUser.getSubject(), oidcUser.getEmail(), name);
        } catch (DisabledException e) {
            // Thrown from inside this success handler rather than the OAuth2 login filter itself, so it
            // would otherwise be caught by ExceptionTranslationFilter and turned into a bare JSON 401 via
            // JwtAuthenticationEntryPoint instead of the redirect every other OAuth2 failure gets.
            oAuth2LoginFailureHandler.onAuthenticationFailure(request, response,
                    new OAuth2AuthenticationException(new OAuth2Error("account_disabled"), e));
            return;
        } finally {
            // The session Spring Security created to hold the OAuth2 authorization request across the
            // Google redirect/callback round trip is no longer needed once the exchange completes.
            var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }

        // Tokens go in the URL fragment, not the query string: the fragment is never sent to any server
        // (ours or Google's) as part of a request, so it can't end up in access logs, Referer headers, or
        // browser/proxy history the way a query param would. It's still readable by the frontend's own
        // JS on the landing page, which is all that's needed here.
        String fragment = result.mfaRequired() ? "mfaRequired=true&mfaToken=" + encode(result.mfaToken())
                : "accessToken=" + encode(result.tokens().accessToken()) + "&refreshToken="
                        + encode(result.tokens().refreshToken());

        String redirectUrl = UriComponentsBuilder.fromUriString(oauth2Properties.redirectUri()).fragment(fragment)
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
