package com.pal.dipesh.razorpay.merchant.service.impl;

import com.pal.dipesh.razorpay.common.enums.UserRole;
import com.pal.dipesh.razorpay.common.exception.DuplicateResourceException;
import com.pal.dipesh.razorpay.merchant.dto.request.LoginRequest;
import com.pal.dipesh.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.MerchantResponse;
import com.pal.dipesh.razorpay.merchant.entity.AppUser;
import com.pal.dipesh.razorpay.merchant.entity.Merchant;
import com.pal.dipesh.razorpay.merchant.mapper.MerchantMapper;
import com.pal.dipesh.razorpay.merchant.repository.AppUserRepository;
import com.pal.dipesh.razorpay.merchant.repository.MerchantRepository;
import com.pal.dipesh.razorpay.merchant.security.JwtUtil;
import com.pal.dipesh.razorpay.merchant.security.rsa.TokenPair;
import com.pal.dipesh.razorpay.merchant.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final MerchantRepository merchantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantMapper merchantMapper;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public MerchantResponse signup(MerchantSignupRequest request) {
        if(merchantRepository.existsByEmail(request.email())) {
            log.warn("Merchant with email {} already exists", request.email());
            throw new DuplicateResourceException("DUPLICATE_MERCHANT_EMAIL", "Merchant with email " + request.email() + " already exists");
        }

        Merchant merchant = merchantMapper.toEntity(request);

        merchant = merchantRepository.save(merchant);

        AppUser appUser = AppUser.builder()
                .email(request.email())
                .merchant(merchant)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .build();

        appUserRepository.save(appUser);

        return merchantMapper.toMerchantResponse(merchant);
    }

    @Override
    public AuthResult login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        AppUser appUser = (AppUser) Objects.requireNonNull(authentication.getPrincipal());

        String email = appUser.getEmail();
        UUID merchantId = appUser.getMerchant().getId();
        List<String> roles = appUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        TokenPair tokens = jwtUtil.generateTokenPair(email, merchantId, roles);

        return new AuthResult(tokens, email, merchantId, roles);
    }

    @Override
    public TokenPair refreshToken() {
        // TODO: Refresh the JWT tokens and generate a new pair of Access Token and Refresh Token

        return null;
    }
}