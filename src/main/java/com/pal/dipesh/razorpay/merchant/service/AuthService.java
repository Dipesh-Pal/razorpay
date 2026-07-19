package com.pal.dipesh.razorpay.merchant.service;

import com.pal.dipesh.razorpay.merchant.dto.request.LoginRequest;
import com.pal.dipesh.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.MerchantResponse;
import com.pal.dipesh.razorpay.merchant.security.rsa.TokenPair;

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

    TokenPair refreshToken();
}
