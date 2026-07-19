package com.pal.dipesh.razorpay.merchant.security.filters;

import com.pal.dipesh.razorpay.merchant.entity.ApiKey;
import com.pal.dipesh.razorpay.merchant.repository.ApiKeyRepository;

import com.pal.dipesh.razorpay.merchant.security.MerchantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String BASIC_PREFIX = "Basic ";
    public static final String ROLE_API_KEY = "ROLE_API_KEY";

    private final MerchantContext merchantContext;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyRepository apiKeyRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Request URI: {}", request.getRequestURI());

        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith(BASIC_PREFIX)) {
                String[] credentials = decode(header);

                if (credentials == null || credentials.length != 2) {
                    throw new BadCredentialsException("Malformed API Key header");
                }

                String keyId = credentials[0];
                String rawSecret = credentials[1];

                ApiKey apiKey = apiKeyRepository.findByKeyId(keyId).orElseThrow(() -> new BadCredentialsException("Invalid or Missing API Key ID"));

                if (!apiKey.isEnabled() || !secretMatches(apiKey, rawSecret)) {
                    throw new BadCredentialsException("API Key is disabled or secret does not match");
                }

                var auth = new UsernamePasswordAuthenticationToken(keyId, null, List.of(new SimpleGrantedAuthority(ROLE_API_KEY)));

                SecurityContextHolder.getContext().setAuthentication(auth);

                merchantContext.setMerchantId(apiKey.getMerchant().getId());
                merchantContext.setKeyId(apiKey.getKeyId());
            }

            log.info("Moving to the next filter");
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Error occurred in ApiKeyAuthenticationFilter: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    private boolean secretMatches(ApiKey apiKey, String rawSecret) {
        if(passwordEncoder.matches(rawSecret, apiKey.getKeySecretHash())) {
            return true;
        }

        LocalDateTime gracePeriod = apiKey.getGracePeriodExpiresAt();
        String previousKeySecretHash = apiKey.getPreviousKeySecretHash();

        return gracePeriod != null && LocalDateTime.now().isBefore(gracePeriod) && passwordEncoder.matches(rawSecret, previousKeySecretHash);
    }

    private String[] decode(String header) {
        String base64Credentials = header.substring(BASIC_PREFIX.length());
        byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
        String decodedCredentials = new String(decodedBytes);

        int colon = decodedCredentials.indexOf(':');

        if(colon < 1) {
            return null;
        }

        return decodedCredentials.split(":", 2);
    }
}
