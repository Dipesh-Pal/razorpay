package com.pal.dipesh.razorpay.merchant.security;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class RedisTokenBlockListService implements TokenBlockListService {

    private static final String JWT_BLOCKLIST_PREFIX = "blocklist:jwt:";
    private static final String MARKER = "1";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> blocklistPairScript;

    public RedisTokenBlockListService(StringRedisTemplate redis) {
        this.redis = redis;
        this.blocklistPairScript = new DefaultRedisScript<>();
        this.blocklistPairScript.setLocation(new ClassPathResource("scripts/blocklist-pair.lua"));
        this.blocklistPairScript.setResultType(Long.class);
    }

    @Override
    public void blockList(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }

        long ttlSeconds = secondsUntil(expiresAt);

        if (ttlSeconds <= 0) {
            log.debug("Skipping blocklist for already-expired jti {}", jti);
            return;
        }

        Boolean wrote = redis.opsForValue().setIfAbsent(key(jti), MARKER, Duration.ofSeconds(ttlSeconds));

        if (Boolean.FALSE.equals(wrote)) {
            // Idempotent replay — original TTL preserved.
            log.debug("jti {} already blocklisted; SET NX skipped", jti);
        }
    }

    @Override
    public long blockListPair(String accessJti, Instant accessExpiresAt, String refreshJti, Instant refreshExpiresAt) {
        String accessKey  = accessJti  == null || accessJti.isBlank()  ? "" : key(accessJti);
        String refreshKey = refreshJti == null || refreshJti.isBlank() ? "" : key(refreshJti);

        // Send 0 to the script to signal "skip this side"; the script's TTL <= 0 guard swallows it.
        String accessTtl  = String.valueOf(accessKey.isEmpty() ? 0L : Math.max(0L, secondsUntil(accessExpiresAt)));
        String refreshTtl = String.valueOf(refreshKey.isEmpty() ? 0L : Math.max(0L, secondsUntil(refreshExpiresAt)));

        // Both sides are no-ops → skip the round trip entirely.
        if ("0".equals(accessTtl) && "0".equals(refreshTtl)) {
            log.debug("blockListPair skipped: both sides null/expired");
            return 0L;
        }

        return redis.execute(
                blocklistPairScript,
                List.of(accessKey, refreshKey),
                accessTtl, refreshTtl
        );
    }

    @Override
    public boolean isBlockListed(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }

        return Boolean.TRUE.equals(redis.hasKey(key(jti)));
    }

    private static String key(String jti) {
        return JWT_BLOCKLIST_PREFIX + jti;
    }

    private static long secondsUntil(Instant expiresAt) {
        if (expiresAt == null) {
            return 0L;
        }

        return Duration.between(Instant.now(), expiresAt).plusMillis(500).toSeconds();
    }
}
