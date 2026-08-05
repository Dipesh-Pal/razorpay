package com.pal.dipesh.razorpay.merchant.security;

import java.time.Instant;

/**
 * Redis-backed blocklist for revoked JWT jtis. Backing keys are TTL'd
 * to the token's remaining lifetime so the store is self-cleaning.
 *
 * <p>All methods are fail-closed: on Redis outage the underlying
 * {@code RedisConnectionFailureException} propagates so callers never
 * silently treat a revoked token as valid.
 */
public interface TokenBlockListService {

    /**
     * Blocklist a single jti until {@code expiresAt}. No-op if the token is
     * already expired (TTL &le; 0) or the jti is already blocklisted
     * (uses SET NX so the original TTL is preserved on replay).
     */
    void blockList(String jti, Instant expiresAt);

    /**
     * Atomically blocklist an access/refresh jti pair in a single Redis
     * round trip via a Lua script. Either side may be {@code null} to skip
     * (e.g. refresh flow when the caller did not present an access token).
     * Each side is subject to the same TTL &le; 0 and SET-NX rules as
     * {@link #blockList(String, Instant)}.
     *
     * @return number of keys actually written (0, 1, or 2).
     */
    long blockListPair(String accessJti, Instant accessExpiresAt, String refreshJti, Instant refreshExpiresAt);

    /**
     * @return true iff {@code jti} is present in the blocklist.
     */
    boolean isBlockListed(String jti);
}

