package com.base.project.spring.boot.usecase;

import org.springframework.transaction.annotation.Transactional;

import com.base.project.spring.boot.domain.User;
import com.base.project.spring.boot.dto.AuthResponse;
import com.base.project.spring.boot.dto.TotpLoginRequest;
import com.base.project.spring.boot.exception.InvalidMfaTokenException;
import com.base.project.spring.boot.exception.InvalidTotpCodeException;
import com.base.project.spring.boot.repository.UserRepository;
import com.base.project.spring.boot.security.jwt.JwtService;
import com.base.project.spring.boot.security.jwt.TokenType;
import com.base.project.spring.boot.security.totp.TotpService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

/**
 * Completes the second step of a TOTP-protected login: exchanges the mfaToken from /login plus a fresh code for a full
 * access+refresh token pair.
 */
@UseCase
@RequiredArgsConstructor
public class VerifyTotpLoginUseCase {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public AuthResponse execute(TotpLoginRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(request.mfaToken(), TokenType.MFA)
                    .orElseThrow(() -> new InvalidMfaTokenException("Token is not an MFA token"));
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidMfaTokenException("MFA token is invalid or expired");
        }

        User user = userRepository.findByEmail(jwtService.extractUsername(claims))
                .orElseThrow(() -> new InvalidMfaTokenException("MFA token no longer maps to a valid account"));

        if (!user.isTotpEnabled() || !totpService.verifyCode(user.getTotpSecret(), request.code())) {
            throw new InvalidTotpCodeException();
        }

        return tokenIssuer.issueFor(user.getEmail(), user.getRole(), user.getId());
    }

}
