package com.pal.dipesh.razorpay.merchant.service;

import com.pal.dipesh.razorpay.merchant.dto.request.LoginRequest;
import com.pal.dipesh.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.MerchantResponse;
import com.pal.dipesh.razorpay.merchant.security.rsa.TokenPair;

import io.jsonwebtoken.Claims;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    record AuthResult(
            TokenPair tokens,
            String email,
            UUID merchantId,
            List<String> roles
    ) {}

    MerchantResponse signup(MerchantSignupRequest merchantRequest);

    AuthResult login(LoginRequest loginRequest);

    /**
     * Rotate a JWT pair. Called from {@code /refresh} after {@code JwtAuthenticationFilter}
     * has already validated the presented credentials in refresh-mode. Fresh roles are
     * re-fetched from the database so demotions/promotions take effect immediately.
     *
     * @param accessTokenClaims  parsed claims from the (optional) Authorization
     *                           header; may be {@code null} if the client only
     *                           sent the refresh cookie.
     * @param refreshTokenClaims parsed claims from the required refresh cookie;
     *                           never {@code null}.
     */
    TokenPair refreshTokens(@Nullable Claims accessTokenClaims, @Nonnull Claims refreshTokenClaims);

    /**
     * Revoke a session on user-initiated logout.
     *
     * <p>The access token is mandatory (its claims come from the request attribute populated
     * by {@code JwtAuthenticationFilter}). The refresh cookie is optional: when absent, the
     * paired refresh JTI is recovered from the access token's {@code rti} claim and
     * blocklisted for a fixed fallback TTL.
     */
    void invalidateTokens(@Nonnull Claims accessTokenClaims, @Nullable String refreshTokenValue);
}

