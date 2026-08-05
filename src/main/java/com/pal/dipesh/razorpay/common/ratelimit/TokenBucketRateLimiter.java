package com.pal.dipesh.razorpay.common.ratelimit;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Token bucket rate limiter: capacity == maxRequests, refill rate is expressed directly in
 * tokens/sec (e.g. 100 rpm => 100/60.0). Unlike the fixed window, tokens trickle back
 * continuously instead of resetting all at once at a window boundary, so it doesn't allow a
 * burst of 2x maxRequests around the edge of two windows. State (tokens remaining, last refill
 * timestamp) lives in a Redis hash; refill + consume is done atomically in a single Lua script
 * so two concurrent requests for the same key can't both read the same "tokens available"
 * snapshot and both be isAllowed through.
 *
 * <p>Fails open — if Redis is unreachable or the script errors, the request is isAllowed rather
 * than blocked. This prefers availability to strict enforcement; flip the {@code failOpen}
 * behavior if your product requires the opposite.
 */
@Slf4j
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:tokenbucket:";
    private static final long DEFAULT_TTL_SECONDS = 3600L;
    // Passing 0 tells the Lua script to fall back to redis.call('TIME') (avoids cross-pod clock skew).
    private static final String USE_SERVER_TIME = "0";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List<Long>> tokenBucketScript;

    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setLocation(new ClassPathResource("scripts/token-bucket-ratelimiter.lua"));

        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<List<Long>> resultType = (Class) List.class;
        this.tokenBucketScript.setResultType(resultType);
    }

    @Override
    public RateLimitResult check(String key, int capacity, double refillPerSec) {
        if (key == null || key.isBlank() || capacity <= 0 || refillPerSec <= 0.0) {
            log.warn("Invalid rate limiter args (key={}, capacity={}, refillPerSec={}); failing open", key, capacity, refillPerSec);
            return RateLimitResult.allowed(Math.max(capacity, 0));
        }

        try {
            List<Long> result = redis.execute(
                    tokenBucketScript,
                    List.of(KEY_PREFIX + key),
                    String.valueOf(capacity),
                    String.valueOf(refillPerSec),
                    USE_SERVER_TIME,
                    String.valueOf(DEFAULT_TTL_SECONDS)
            );

            if (result == null || result.size() < 3) {
                log.warn("Token bucket script returned unexpected result for key {}: {}", key, result);
                return RateLimitResult.allowed(capacity);
            }

            boolean allowed = result.get(0) != null && result.get(0) == 1L;
            int remaining = result.get(1) == null ? 0 : result.get(1).intValue();
            long retryAfterMs = result.get(2) == null ? 0L : result.get(2);

            return allowed ? RateLimitResult.allowed(remaining) : RateLimitResult.denied(remaining, retryAfterMs);
        } catch (Exception e) {
            log.error("Rate limiter error for key {} — failing open: {}", key, e.getMessage(), e);
            return RateLimitResult.allowed(capacity);
        }
    }
}
