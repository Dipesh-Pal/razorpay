package com.pal.dipesh.razorpay.merchant.security.filters;

import com.pal.dipesh.razorpay.common.exception.InvalidRefreshTokenException;
import com.pal.dipesh.razorpay.common.util.CookieUtil;
import com.pal.dipesh.razorpay.merchant.security.AppUserContext;
import com.pal.dipesh.razorpay.merchant.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String REFRESH_TOKEN_CLAIMS_ATTR = "REFRESH_TOKEN_CLAIMS";
    public static final String ACCESS_TOKEN_CLAIMS_ATTR = "ACCESS_TOKEN_CLAIMS";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String ROLE_REFRESH = "ROLE_REFRESH";

    private final JwtUtil jwtUtil;
    private final AppUserContext appUserContext;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.info("Request URI: {}", requestURI);

        try {
            if (requestURI.contains("/auth/refresh")) {
                refreshMode(request);
            } else {
                normalMode(request);
            }

            log.info("Moving to the next filter");
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Error occurred in JwtAuthenticationFilter: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    /**
     * Refresh-mode contract:
     *   1. Refresh token in the {@code refresh_token} cookie is REQUIRED. Fully validated
     *      (signature, type, expiry, blocklist).
     *   2. Access token in {@code Authorization: Bearer …} header is OPTIONAL. If present,
     *      and if it is Fully valid (signature, type, expiry, blocklist), then its
     *      {@code sub} must equal the refresh token's {@code sub}. Otherwise, an
     *      {@link InvalidRefreshTokenException} is thrown. If not present or not valid,
     *      Access Token is ignored.
     *   3. On success, both {@link Claims} objects are stashed as request attributes for the
     *      controller/service, and the {@link SecurityContextHolder} is populated from the
     *      REFRESH claims so downstream {@code authenticated()} rules pass.
     */
    private void refreshMode(HttpServletRequest request) {
        String refreshToken = readRefreshCookie(request);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("REFRESH_TOKEN_MISSING", "Refresh token cookie is missing");
        }

        Claims refreshClaims;

        try {
            refreshClaims = jwtUtil.validateRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("REFRESH_TOKEN_INVALID", "Refresh token is invalid: " + e.getMessage(), e);
        }

        Claims accessClaims = null;
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());

            try {
                accessClaims = jwtUtil.validateAccessToken(accessToken);
            } catch (JwtException e) {
                log.warn("Ignoring invalid access token in refresh flow: {}", accessToken, e);
            }

            if (accessClaims != null && !Objects.equals(jwtUtil.extractSubject(refreshClaims), jwtUtil.extractSubject(accessClaims))) {
                log.warn("Refresh subject mismatch: refreshSub={} accessSub={} (possible token-mixing attack)", jwtUtil.extractSubject(refreshClaims), jwtUtil.extractSubject(accessClaims));
                throw new InvalidRefreshTokenException("TOKEN_PAIR_SUBJECT_MISMATCH", "Access token and refresh token belong to different subjects");
            }
        }

        var auth = new UsernamePasswordAuthenticationToken(jwtUtil.extractSubject(refreshClaims), null, List.of(new SimpleGrantedAuthority(ROLE_REFRESH)));
        SecurityContextHolder.getContext().setAuthentication(auth);

        appUserContext.setMerchantId(jwtUtil.extractMerchantId(refreshClaims));
        appUserContext.setUsername(jwtUtil.extractSubject(refreshClaims));

        request.setAttribute(REFRESH_TOKEN_CLAIMS_ATTR, refreshClaims);
        request.setAttribute(ACCESS_TOKEN_CLAIMS_ATTR, accessClaims); // nullable
    }

    public static String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (CookieUtil.REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void normalMode(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());

            Claims claims = jwtUtil.validateAccessToken(token);

            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var auth = new UsernamePasswordAuthenticationToken(jwtUtil.extractSubject(claims), null, jwtUtil.extractRoles(claims));
                SecurityContextHolder.getContext().setAuthentication(auth);

                appUserContext.setMerchantId(jwtUtil.extractMerchantId(claims));
                appUserContext.setUsername(jwtUtil.extractSubject(claims));

                request.setAttribute(ACCESS_TOKEN_CLAIMS_ATTR, claims);
            }
        }
    }
}
