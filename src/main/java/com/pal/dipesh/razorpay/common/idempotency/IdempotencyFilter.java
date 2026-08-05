package com.pal.dipesh.razorpay.common.idempotency;

import com.pal.dipesh.razorpay.common.exception.IdemPotencyConflictException;
import com.pal.dipesh.razorpay.merchant.security.AppUserContext;
import com.pal.dipesh.razorpay.merchant.security.MerchantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> GUARDED_METHODS = Set.of("POST", "PUT", "DELETE");
    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final Duration IN_PROGRESS_TTL = Duration.ofSeconds(30);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);
    private static final String SEPARATOR = "|";

    private final AppUserContext appUserContext;
    private final MerchantContext merchantContext;
    private final IdempotencyStore idempotencyStore;
    private final HandlerExceptionResolver exceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(!GUARDED_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if(idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID merchantId = merchantContext != null ? merchantContext.getMerchantId() : appUserContext.getMerchantId();
        String key = merchantId != null ? merchantId+":"+idempotencyKey : idempotencyKey;

        boolean claimed = idempotencyStore.setIfAbsent(key, IN_PROGRESS_TTL);

        if(!claimed) {
            // Another thread has already claimed this key
            Optional<String> existingResponse = idempotencyStore.get(key);

            if(existingResponse.isPresent() && !IdempotencyStore.IN_PROGRESS.equals(existingResponse.get())) {
                // Actual response is stored inside redis
                replay(request, response, existingResponse.get());
            } else {
                // Key is in progress, return 409 Conflict
                var ex = new IdemPotencyConflictException("A Request with this Idempotency-Key is already in progress.");
                exceptionResolver.resolveException(request, response, null, ex);
            }

            return;
        }

        // first time claim
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            int status = wrappedResponse.getStatus();
            byte[] bodyBytes = wrappedResponse.getContentAsByteArray();
            String body = new String(bodyBytes, StandardCharsets.UTF_8);

            if(status < 400 && bodyBytes.length > 0) {
                // Success - store the response in the idempotency store for future replays
                String stored = status + SEPARATOR + body;
                idempotencyStore.store(key, stored, COMPLETED_TTL);
                log.debug("Stored Idempotency Key={}, Stored Value={}", key, stored);
            } else {
                // Error or empty - delete the key to allow fresh retries
                idempotencyStore.delete(key);
                log.debug("Deleted Idempotency Key={} due to failure. Error status={}", key, status);
            }

            wrappedResponse.copyBodyToResponse();
        }
    }

    private void replay(HttpServletRequest request, HttpServletResponse response, String stored) throws IOException {
        int separatorIndex = stored.indexOf(SEPARATOR);

        if(separatorIndex < 0) {
            log.warn("Malformed stored value for Idempotency Key: {}", stored);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        int status = Integer.parseInt(stored.substring(0, separatorIndex));
        String body = stored.substring(separatorIndex + 1);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
