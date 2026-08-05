package com.pal.dipesh.razorpay.common.ratelimit;

public record RateLimitResult(boolean isAllowed, int remaining, long retryAfterMs) {

    public static RateLimitResult allowed(int remaining) {
        return new RateLimitResult(true, remaining, 0L);
    }

    public static RateLimitResult denied(int remaining, long retryAfterMs) {
        return new RateLimitResult(false, remaining, retryAfterMs);
    }

    /** HTTP {@code Retry-After} header wants whole seconds; round up so callers never retry too early. */
    public long retryAfterSeconds() {
        if (retryAfterMs <= 0L) {
            return 0L;
        }

        return (retryAfterMs + 999L) / 1000L;
    }
}

