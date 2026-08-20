package com.pal.dipesh.razorpay.operations.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryQueue {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.webhook.delivery.redis-key:webhook-retry}")
    private String key;

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> POLL_DUE_SCRIPT = new DefaultRedisScript<>(
            """
            local due = redis.call('ZRANGE', KEYS[1], 0, ARGV[1], 'BYSCORE', 'LIMIT', 0, ARGV[2])
            if #due > 0 then
                redis.call('ZREM', KEYS[1], unpack(due))
            end
            return due
            """,
            List.class
    );

    @SuppressWarnings("unchecked")
    public Set<UUID> pollDue(int limit) {
        List<String> ids = redisTemplate.execute(
                POLL_DUE_SCRIPT,
                List.of(key),
                String.valueOf(getTime(LocalDateTime.now())),
                String.valueOf(limit)
        );

        if (ids.isEmpty()) {
            log.debug("No due webhook events found in retry queue");
            return Set.of();
        }

        log.info("Polled {} due webhook events from retry queue", ids.size());

        return ids.stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    public void enqueueIfAbsent(UUID webhookEventId, LocalDateTime nextRetryAt) {
        Boolean added = redisTemplate.opsForZSet().addIfAbsent(key, webhookEventId.toString(), getTime(nextRetryAt));

        if (Boolean.TRUE.equals(added)) {
            log.info("Enqueued webhook event {} for retry at {}", webhookEventId, nextRetryAt);
        } else {
            log.debug("Webhook event {} already scheduled, keeping existing entry", webhookEventId);
        }
    }

    private long getTime(LocalDateTime retryAt) {
        return retryAt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
