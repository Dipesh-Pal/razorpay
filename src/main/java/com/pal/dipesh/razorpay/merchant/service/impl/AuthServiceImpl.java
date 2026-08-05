package com.pal.dipesh.razorpay.merchant.service.impl;

import com.pal.dipesh.razorpay.common.enums.UserRole;
import com.pal.dipesh.razorpay.common.exception.DuplicateResourceException;
import com.pal.dipesh.razorpay.common.exception.InvalidRefreshTokenException;
import com.pal.dipesh.razorpay.common.exception.ResourceNotFoundException;
import com.pal.dipesh.razorpay.merchant.dto.request.LoginRequest;
import com.pal.dipesh.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.MerchantResponse;
import com.pal.dipesh.razorpay.merchant.entity.AppUser;
import com.pal.dipesh.razorpay.merchant.entity.Merchant;
import com.pal.dipesh.razorpay.merchant.mapper.MerchantMapper;
import com.pal.dipesh.razorpay.merchant.repository.AppUserRepository;
import com.pal.dipesh.razorpay.merchant.repository.MerchantRepository;
import com.pal.dipesh.razorpay.merchant.security.JwtUtil;
import com.pal.dipesh.razorpay.merchant.security.TokenBlockListService;
import com.pal.dipesh.razorpay.merchant.security.rsa.TokenPair;
import com.pal.dipesh.razorpay.merchant.service.AuthService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Duration REFRESH_BLOCKLIST_FALLBACK_TTL = Duration.ofHours(24);

    private final AuthenticationManager authenticationManager;
    private final TokenBlockListService blockListService;
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
    public TokenPair refreshTokens(@Nullable Claims accessTokenClaims, @Nonnull Claims refreshTokenClaims) {
        String email = jwtUtil.extractSubject(refreshTokenClaims);

        AppUser user = appUserRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("AppUser", email));

        UUID merchantId = user.getMerchant().getId();
        List<String> freshRoles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        TokenPair newPair = jwtUtil.generateTokenPair(email, merchantId, freshRoles);

        // Revoke the pair that was just presented so the same refresh can't be replayed.
        jwtUtil.invalidateTokens(accessTokenClaims, refreshTokenClaims);

        return newPair;
    }

    @Override
    public void invalidateTokens(@Nonnull Claims accessTokenClaims, @Nullable String refreshTokenValue) {
        Objects.requireNonNull(accessTokenClaims, "accessTokenClaims required for logout");

        String accessJti = jwtUtil.extractJti(accessTokenClaims);
        Instant accessExp = jwtUtil.extractExpiry(accessTokenClaims);
        String accessSub = jwtUtil.extractSubject(accessTokenClaims);
        String rti = jwtUtil.extractRefreshJti(accessTokenClaims);

        Instant primaryRefreshExp = rti == null ? null : Instant.now().plus(REFRESH_BLOCKLIST_FALLBACK_TTL);

        String secondaryRefreshJti = null;
        Instant secondaryRefreshExp = null;

        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            try {
                Claims cookieClaims = jwtUtil.parseRefreshForLogout(refreshTokenValue);

                String cookieJti = jwtUtil.extractJti(cookieClaims);
                String cookieSub = jwtUtil.extractSubject(cookieClaims);
                Instant cookieExp = jwtUtil.extractExpiry(cookieClaims);

                if (!Objects.equals(cookieSub, accessSub)) {
                    log.warn("Logout token-mixing detected: accessSub={} cookieSub={} — ignoring cookie", accessSub, cookieSub);
                } else if (Objects.equals(cookieJti, rti)) {
                    primaryRefreshExp = cookieExp;
                } else {
                    // Same user, different session — blocklist both so neither can be reused.
                    log.warn("Logout session mismatch for sub={}: accessRti={} cookieJti={} — blocklisting both", accessSub, rti, cookieJti);
                    secondaryRefreshJti = cookieJti;
                    secondaryRefreshExp = cookieExp;
                }
            } catch (JwtException e) {
                log.warn("Refresh cookie unparseable at logout — falling back to rti + {}h TTL: {}", REFRESH_BLOCKLIST_FALLBACK_TTL.toHours(), e.getMessage());
            }
        }

        if (rti == null && secondaryRefreshJti == null) {
            log.warn("Access token has no rti claim and no refresh cookie present — refresh side not blocklisted (accessSub={})", accessSub);
        }

        blockListService.blockListPair(accessJti, accessExp, rti, primaryRefreshExp);

        if (secondaryRefreshJti != null) {
            blockListService.blockList(secondaryRefreshJti, secondaryRefreshExp);
        }
    }
}
