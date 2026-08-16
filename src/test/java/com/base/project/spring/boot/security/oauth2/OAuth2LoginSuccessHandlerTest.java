package com.base.project.spring.boot.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.base.project.spring.boot.config.OAuth2Properties;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.usecase.GoogleLoginUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private GoogleLoginUseCase googleLoginUseCase;

    @Mock
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OidcUser oidcUser;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginSuccessHandler(googleLoginUseCase, oAuth2LoginFailureHandler,
                new OAuth2Properties("http://localhost:3000/oauth2/callback"));
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getSubject()).thenReturn("google-123");
        when(oidcUser.getEmail()).thenReturn("rafael@example.com");
        when(oidcUser.getFullName()).thenReturn("Rafael");
    }

    @Test
    void onAuthenticationSuccess_withFullTokens_putsThemInTheFragmentNotTheQueryString() throws Exception {
        AuthResponse tokens = new AuthResponse("access-token", "refresh-token", "Bearer", 900);
        when(googleLoginUseCase.execute("google-123", "rafael@example.com", "Rafael"))
                .thenReturn(LoginResponse.authenticated(tokens));

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectUrl = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectUrl.capture());
        String url = redirectUrl.getValue();
        assertThat(url).startsWith("http://localhost:3000/oauth2/callback#");
        assertThat(url).doesNotContain("?accessToken=").doesNotContain("?refreshToken=");
        assertThat(url).contains("accessToken=access-token").contains("refreshToken=refresh-token");
    }

    @Test
    void onAuthenticationSuccess_whenMfaRequired_redirectsWithMfaTokenInsteadOfLiveTokens() throws Exception {
        when(googleLoginUseCase.execute("google-123", "rafael@example.com", "Rafael"))
                .thenReturn(LoginResponse.mfaRequired("mfa-token"));

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectUrl = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectUrl.capture());
        String url = redirectUrl.getValue();
        assertThat(url).contains("#mfaRequired=true").contains("mfaToken=mfa-token");
        assertThat(url).doesNotContain("accessToken=");
    }

    @Test
    void onAuthenticationSuccess_whenAccountDisabled_delegatesToFailureHandlerInsteadOfLeakingA401() throws Exception {
        when(googleLoginUseCase.execute("google-123", "rafael@example.com", "Rafael"))
                .thenThrow(new DisabledException("Account is disabled"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(oAuth2LoginFailureHandler).onAuthenticationFailure(any(), any(), any());
        verify(response, never()).sendRedirect(any());
    }

}
