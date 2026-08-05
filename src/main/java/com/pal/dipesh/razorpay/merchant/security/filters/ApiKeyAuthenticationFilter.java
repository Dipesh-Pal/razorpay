package com.pal.dipesh.razorpay.merchant.security.filters;

import com.pal.dipesh.razorpay.common.exception.RateLimitExceededException;
import com.pal.dipesh.razorpay.common.ratelimit.RateLimitResult;
import com.pal.dipesh.razorpay.common.ratelimit.RateLimiter;
import com.pal.dipesh.razorpay.merchant.cache.ApiKeyCache;
import com.pal.dipesh.razorpay.merchant.cache.ApiKeyCacheEntry;
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
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String BASIC_PREFIX = "Basic ";
    public static final String ROLE_API_KEY = "ROLE_API_KEY";

    // Per-API-key quota: 100 requests / minute (burst of 100, refill ~1.667 tokens/sec).
    private static final int API_KEY_CAPACITY = 100; // TODO: externalize via @ConfigurationProperties once we have per-tier plans.
    private static final double API_KEY_REFILL_PER_SEC = 100.0 / 60.0;
    private static final String RATE_LIMIT_KEY_PREFIX = "apikey:";

    private final ApiKeyCache apiKeyCache;
    private final RateLimiter rateLimiter;
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

                ApiKeyCacheEntry cachedEntry = apiKeyCache.get(keyId).orElseGet(() -> loadAndCache(keyId));

                if(cachedEntry == null){
                    throw new BadCredentialsException("Invalid or Missing API Key");
                }

                if (!cachedEntry.enabled() || !secretMatches(cachedEntry, rawSecret)) {
                    throw new BadCredentialsException("API Key is disabled or secret does not match");
                }

                enforceRateLimit(keyId, response);

                var auth = new UsernamePasswordAuthenticationToken(keyId, null, List.of(new SimpleGrantedAuthority(ROLE_API_KEY)));

                SecurityContextHolder.getContext().setAuthentication(auth);

                merchantContext.setMerchantId(cachedEntry.merchantId());
                merchantContext.setKeyId(cachedEntry.keyId());
            }

            log.info("Moving to the next filter");
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Error occurred in ApiKeyAuthenticationFilter: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    private void enforceRateLimit(String keyId, HttpServletResponse response) {
        RateLimitResult result = rateLimiter.check(RATE_LIMIT_KEY_PREFIX + keyId, API_KEY_CAPACITY, API_KEY_REFILL_PER_SEC);

        response.setHeader("X-RateLimit-Limit", String.valueOf(API_KEY_CAPACITY));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining())));

        if (!result.isAllowed()) {
            long retryAfterSeconds = result.retryAfterSeconds();
            log.warn("Rate limit exceeded for apiKeyId={}, retryAfterSeconds={}", keyId, retryAfterSeconds);

            throw new RateLimitExceededException(
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests for this API key. Retry after " + retryAfterSeconds + "s.",
                    retryAfterSeconds,
                    Math.max(0, result.remaining())
            );
        }
    }

    private ApiKeyCacheEntry loadAndCache(String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyId(keyId).orElse(null);

        if (apiKey == null) {
            return null;
        }

        ApiKeyCacheEntry entry = new ApiKeyCacheEntry(
                apiKey.getKeyId(),
                apiKey.getKeySecretHash(),
                apiKey.getPreviousKeySecretHash(),
                apiKey.getGracePeriodExpiresAt(),
                apiKey.getMerchant().getId(),
                apiKey.getEnvironment(),
                apiKey.isEnabled()
        );

        apiKeyCache.put(keyId, entry);
        return entry;
    }

    private boolean secretMatches(ApiKeyCacheEntry cacheEntry, String rawSecret) {
        if(passwordEncoder.matches(rawSecret, cacheEntry.keySecretHash())) {
            return true;
        }

        String previousKeySecretHash = cacheEntry.previousKeySecretHash();

        return cacheEntry.isInGracePeriod() && passwordEncoder.matches(rawSecret, previousKeySecretHash);
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
