package com.pal.dipesh.razorpay.common.util;

import com.pal.dipesh.razorpay.merchant.security.CookieProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final CookieProperties cookieProperties;

    public ResponseCookie buildRefreshCookie(String refreshToken, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0,
                Duration.between(Instant.now(), expiresAt).plusMillis(500).toSeconds());

        return baseBuilder(refreshToken)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return baseBuilder("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path());
    }
}
