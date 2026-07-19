package com.pal.dipesh.razorpay.merchant.security.filters;

import com.pal.dipesh.razorpay.merchant.security.AppUserContext;
import com.pal.dipesh.razorpay.merchant.security.JwtUtil;
import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final AppUserContext appUserContext;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Request URI: {}", request.getRequestURI());

        try {
            final String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
                String token = authorizationHeader.substring(BEARER_PREFIX.length());

                Claims claims = jwtUtil.validateAccessToken(token);

                if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, jwtUtil.extractRoles(claims));

                    SecurityContextHolder.getContext().setAuthentication(auth);

                    appUserContext.setMerchantId(jwtUtil.extractMerchantId(claims));
                    appUserContext.setUsername(claims.getSubject());
                }
            }

            log.info("Moving to the next filter");
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Error occurred in JwtAuthenticationFilter: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
