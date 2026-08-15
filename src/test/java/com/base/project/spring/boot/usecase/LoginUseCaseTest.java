package com.base.project.spring.boot.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.base.project.spring.boot.domain.Role;
import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.LoginRequest;
import com.base.project.spring.boot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenIssuer tokenIssuer;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(authenticationManager, userRepository, tokenIssuer);
    }

    @Test
    void execute_whenCredentialsValid_authenticatesAndIssuesTokens() {
        LoginRequest request = new LoginRequest("rafael@example.com", "supersecret123");
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("rafael@example.com").role(Role.USER).enabled(true).build();
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.of(user));
        AuthResponse expected = new AuthResponse("access", "refresh", "Bearer", 900);
        when(tokenIssuer.issueFor("rafael@example.com", Role.USER, userId)).thenReturn(expected);

        AuthResponse result = useCase.execute(request);

        assertThat(result).isEqualTo(expected);
        verify(authenticationManager)
                .authenticate(eq(new UsernamePasswordAuthenticationToken("rafael@example.com", "supersecret123")));
    }

    @Test
    void execute_whenAuthenticationFails_propagatesException() {
        LoginRequest request = new LoginRequest("rafael@example.com", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void execute_whenUserVanishesAfterAuthentication_throwsIllegalState() {
        LoginRequest request = new LoginRequest("rafael@example.com", "supersecret123");
        when(userRepository.findByEmail("rafael@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(IllegalStateException.class);
    }

}
