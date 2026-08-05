package com.pal.dipesh.razorpay.merchant.controller;

import com.pal.dipesh.razorpay.common.annotation.ResponseMessage;
import com.pal.dipesh.razorpay.common.enums.TokenType;
import com.pal.dipesh.razorpay.common.util.CookieUtil;
import com.pal.dipesh.razorpay.merchant.dto.request.LoginRequest;
import com.pal.dipesh.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.LoginResponse;
import com.pal.dipesh.razorpay.merchant.dto.response.MerchantResponse;
import com.pal.dipesh.razorpay.merchant.dto.response.TokenRefreshResponse;
import com.pal.dipesh.razorpay.merchant.security.filters.JwtAuthenticationFilter;
import com.pal.dipesh.razorpay.merchant.security.rsa.TokenPair;
import com.pal.dipesh.razorpay.merchant.service.AuthService;
import com.pal.dipesh.razorpay.merchant.service.AuthService.AuthResult;

import io.jsonwebtoken.Claims;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/signup")
    @ResponseMessage("Merchant signup successful")
    public ResponseEntity<MerchantResponse> signup(@RequestBody @Valid MerchantSignupRequest merchantRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.signup(merchantRequest));
    }

    @PostMapping("/login")
    @ResponseMessage("Login successful")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        AuthResult result = authService.login(loginRequest);
        TokenPair tokens = result.tokens();

        ResponseCookie refreshCookie = cookieUtil.buildRefreshCookie(
                tokens.refreshToken(),
                tokens.refreshTokenExpiresAt()
        );

        Instant now = Instant.now();
        LoginResponse body = new LoginResponse(
                tokens.accessToken(),
                TokenType.ACCESS,
                secondsUntil(now, tokens.accessTokenExpiresAt()),
                secondsUntil(now, tokens.refreshTokenExpiresAt()),
                result.merchantId(),
                result.email(),
                result.roles()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    private static long secondsUntil(Instant now, Instant expiresAt) {
        return Duration.between(now, expiresAt).plusMillis(500).toSeconds();
    }

    @PostMapping("/refresh")
    @ResponseMessage("Access Token Refreshed")
    public ResponseEntity<TokenRefreshResponse> refreshToken(HttpServletRequest request) {
        Claims refreshClaims = (Claims) request.getAttribute(JwtAuthenticationFilter.REFRESH_TOKEN_CLAIMS_ATTR);
        Claims accessClaims  = (Claims) request.getAttribute(JwtAuthenticationFilter.ACCESS_TOKEN_CLAIMS_ATTR);

        TokenPair tokens = authService.refreshTokens(accessClaims, refreshClaims);

        ResponseCookie refreshCookie = cookieUtil.buildRefreshCookie(
                tokens.refreshToken(),
                tokens.refreshTokenExpiresAt()
        );

        Instant now = Instant.now();
        TokenRefreshResponse body = new TokenRefreshResponse(
                tokens.accessToken(),
                TokenType.ACCESS,
                secondsUntil(now, tokens.accessTokenExpiresAt()),
                secondsUntil(now, tokens.refreshTokenExpiresAt())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    @PostMapping("/logout")
    @ResponseMessage("Logged Out Successfully")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        Claims accessClaims = (Claims) request.getAttribute(JwtAuthenticationFilter.ACCESS_TOKEN_CLAIMS_ATTR);
        String refreshCookieValue = JwtAuthenticationFilter.readRefreshCookie(request);

        authService.invalidateTokens(accessClaims, refreshCookieValue);

        SecurityContextHolder.clearContext();

        ResponseCookie deleteCookie = cookieUtil.clearRefreshCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }
}