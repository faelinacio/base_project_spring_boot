package com.base.project.spring.boot.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.LoginRequest;
import com.base.project.spring.boot.dto.LoginResponse;
import com.base.project.spring.boot.dto.RefreshRequest;
import com.base.project.spring.boot.dto.RegisterRequest;
import com.base.project.spring.boot.dto.TotpCodeRequest;
import com.base.project.spring.boot.dto.TotpLoginRequest;
import com.base.project.spring.boot.dto.TotpSetupResponse;
import com.base.project.spring.boot.dto.UserResponse;
import com.base.project.spring.boot.exception.ErrorResponse;
import com.base.project.spring.boot.repository.UserRepository;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the full HTTP stack (controllers, security filter chain, exception handling, use cases, JPA) against a real
 * Postgres, mirroring the register/login/refresh/logout flow a real client would drive.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private AuthResponse register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", email, "supersecret123");
        String body = mockMvc
                .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, AuthResponse.class);
    }

    @Test
    void register_returnsAccessAndRefreshTokens() throws Exception {
        AuthResponse response = register(uniqueEmail());

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isPositive();
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email);

        RegisterRequest request = new RegisterRequest("Test User", email, "supersecret123");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());
    }

    @Test
    void register_invalidPayload_returns400WithFieldErrors() throws Exception {
        RegisterRequest request = new RegisterRequest("", "not-an-email", "123");

        String body = mockMvc
                .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();

        ErrorResponse response = objectMapper.readValue(body, ErrorResponse.class);
        assertThat(response.fieldErrors()).containsKeys("name", "email", "password");
    }

    @Test
    void getCurrentUser_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_withValidToken_returnsUserProfile() throws Exception {
        String email = uniqueEmail();
        AuthResponse tokens = register(email);
        verifyEmail(email);

        String body = mockMvc
                .perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UserResponse response = objectMapper.readValue(body, UserResponse.class);
        assertThat(response.email()).isEqualTo(email);
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void getCurrentUser_withUnverifiedEmailToken_returns403() throws Exception {
        AuthResponse tokens = register(uniqueEmail());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withUserRole_returns403() throws Exception {
        String email = uniqueEmail();
        AuthResponse tokens = register(email);
        verifyEmail(email);

        mockMvc.perform(get("/api/admin/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withAdminRole_returns200() throws Exception {
        String email = uniqueEmail();
        AuthResponse tokens = register(email);
        verifyEmail(email);
        promoteToAdmin(email);

        mockMvc.perform(get("/api/admin/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        register(email);

        LoginRequest request = new LoginRequest(email, "wrongpassword");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnverifiedEmail_returns403() throws Exception {
        String email = uniqueEmail();
        register(email);

        LoginRequest request = new LoginRequest(email, "supersecret123");
        String body = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();

        ErrorResponse response = objectMapper.readValue(body, ErrorResponse.class);
        assertThat(response.message()).isEqualTo("Please verify your email before logging in");
    }

    @Test
    void login_withCorrectPasswordAndVerifiedEmail_returnsFreshTokens() throws Exception {
        String email = uniqueEmail();
        register(email);
        verifyEmail(email);

        LoginRequest request = new LoginRequest(email, "supersecret123");
        String body = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        LoginResponse response = objectMapper.readValue(body, LoginResponse.class);
        assertThat(response.mfaRequired()).isFalse();
        assertThat(response.tokens().accessToken()).isNotBlank();
    }

    @Test
    void verifyEmail_withInvalidToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"not-a-real-token\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void resendVerification_forUnknownEmail_stillReturns202() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + uniqueEmail() + "\"}")).andExpect(status().isAccepted());
    }

    @Test
    void totpFlow_setupEnableAndLoginRequiresCode() throws Exception {
        String email = uniqueEmail();
        AuthResponse tokens = register(email);
        verifyEmail(email);

        String setupBody = mockMvc
                .perform(post("/api/users/me/totp/setup").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        TotpSetupResponse setup = objectMapper.readValue(setupBody, TotpSetupResponse.class);
        assertThat(setup.secret()).isNotBlank();

        String code = new DefaultCodeGenerator().generate(setup.secret(), Instant.now().getEpochSecond() / 30);

        mockMvc.perform(
                post("/api/users/me/totp/enable").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TotpCodeRequest(code))))
                .andExpect(status().isNoContent());

        LoginRequest loginRequest = new LoginRequest(email, "supersecret123");
        String loginBody = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(loginBody, LoginResponse.class);
        assertThat(loginResponse.mfaRequired()).isTrue();
        assertThat(loginResponse.mfaToken()).isNotBlank();

        String freshCode = new DefaultCodeGenerator().generate(setup.secret(), Instant.now().getEpochSecond() / 30);
        TotpLoginRequest totpLoginRequest = new TotpLoginRequest(loginResponse.mfaToken(), freshCode);
        mockMvc.perform(post("/api/auth/login/totp").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(totpLoginRequest))).andExpect(status().isOk());
    }

    @Test
    void refresh_rotatesTokenAndInvalidatesThePrevious() throws Exception {
        AuthResponse initial = register(uniqueEmail());
        RefreshRequest refreshRequest = new RefreshRequest(initial.refreshToken());

        String body = mockMvc
                .perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        AuthResponse rotated = objectMapper.readValue(body, AuthResponse.class);
        assertThat(rotated.refreshToken()).isNotEqualTo(initial.refreshToken());

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))).andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_onDisabledAccount_returns401() throws Exception {
        String email = uniqueEmail();
        AuthResponse tokens = register(email);
        disableAccount(email);

        RefreshRequest refreshRequest = new RefreshRequest(tokens.refreshToken());
        String body = mockMvc
                .perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        ErrorResponse response = objectMapper.readValue(body, ErrorResponse.class);
        assertThat(response.message()).isEqualTo("Account is disabled");
    }

    @Test
    void logout_revokesTheRefreshToken() throws Exception {
        AuthResponse tokens = register(uniqueEmail());
        RefreshRequest refreshRequest = new RefreshRequest(tokens.refreshToken());

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))).andExpect(status().isUnauthorized());
    }

    private void promoteToAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    private void disableAccount(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);
    }

    private void verifyEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

}
