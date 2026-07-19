package com.pal.dipesh.razorpay.merchant.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.cookie")
public record CookieProperties(
        @NotBlank String path,
        @Pattern(regexp = "Strict|Lax|None") String sameSite,
        boolean secure
) {
}
