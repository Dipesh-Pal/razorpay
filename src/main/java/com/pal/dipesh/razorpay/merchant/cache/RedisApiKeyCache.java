package com.pal.dipesh.razorpay.merchant.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisApiKeyCache implements ApiKeyCache {

    private static final String API_KEY_CACHE_PREFIX = "cache:apikey:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ApiKeyCacheEntry> get(String keyId) {
        try{
            String json = stringRedisTemplate.opsForValue().get(API_KEY_CACHE_PREFIX + keyId);

            if(json == null){
                return Optional.empty();
            }

            ApiKeyCacheEntry entry = objectMapper.readValue(json, ApiKeyCacheEntry.class);
            return Optional.of(entry);
        } catch (Exception e) {
            log.error("Error fetching API key from Redis cache for keyId {}: {}", keyId, e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public void put(String keyId, ApiKeyCacheEntry entry) {
        try{
            stringRedisTemplate.opsForValue().set(API_KEY_CACHE_PREFIX + keyId, objectMapper.writeValueAsString(entry), TTL);
        } catch (Exception e) {
            log.error("Error putting API key into Redis cache for keyId {}: {}", keyId, e.getMessage(), e);
        }
    }

    @Override
    public void evict(String keyId) {
        try{
            stringRedisTemplate.delete(API_KEY_CACHE_PREFIX + keyId);
        } catch (Exception e) {
            log.error("Error evicting API key from Redis cache for keyId {}: {}", keyId, e.getMessage(), e);
        }
    }
}
