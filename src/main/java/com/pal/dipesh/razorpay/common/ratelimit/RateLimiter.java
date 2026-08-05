package com.pal.dipesh.razorpay.common.ratelimit;

public interface RateLimiter {

    /**
     * Attempt to consume one unit of quota against a bucket.
     *
     * @param key           bucket identifier (e.g. "apikey:xyz"); the implementation adds its own namespace prefix
     * @param capacity      max burst size — the bucket can never hold more than this many tokens
     * @param refillPerSec  steady-state refill rate in tokens/second (maybe fractional, e.g. 100/60.0 for 100 rpm)
     * @return outcome including remaining tokens and, on denial, how long to wait
     */
    RateLimitResult check(String key, int capacity, double refillPerSec);
}

