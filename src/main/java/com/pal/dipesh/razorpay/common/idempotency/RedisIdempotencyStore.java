package com.pal.dipesh.razorpay.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IdempotencyStore{

    private static final String REDIS_KEY_PREFIX = "idempotency:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean setIfAbsent(String key, Duration ttl) {
        try{
            Boolean set = redisTemplate.opsForValue().setIfAbsent(REDIS_KEY_PREFIX + key, IN_PROGRESS, ttl);
            return Boolean.TRUE.equals(set);
        } catch (DataAccessException e) {
            log.warn("Idempotency store unavailable, failing open for key={}", key, e);
            return true;
        }
    }

    @Override
    public void store(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + key, value, ttl);
        } catch (DataAccessException e) {
            log.warn("Failed to persist in IdempotencyStore, failing open for key={}", key, e);
        }
    }

    @Override
    public Optional<String> get(String key) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + key));
        } catch (Exception e) {
            log.warn("Failed to read from IdempotencyStore, failing open for key={}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(REDIS_KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("Failed to delete from IdempotencyStore, failing open for key={}", key, e);
        }
    }
}
