package com.pal.dipesh.razorpay.operations.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pal.dipesh.razorpay.common.pojo.WebhookTarget;
import com.pal.dipesh.razorpay.common.enums.WebhookEventStatus;
import com.pal.dipesh.razorpay.common.util.SignerUtil;
import com.pal.dipesh.razorpay.merchant.api.MerchantLookupService;
import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;
import com.pal.dipesh.razorpay.operations.repository.InboxEventRepository;
import com.pal.dipesh.razorpay.operations.repository.WebhookEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes a Kafka envelope from the payment/order/refund/settlement topics
 * and fans it out to the merchant's active webhook targets.
 *
 * <p>Idempotent against Kafka redelivery: the first action is a
 * {@code tryInsert} into {@code inbox_event}. A duplicate delivery returns
 * an empty list <strong>before</strong> the merchant-configuration lookup
 * ({@link MerchantLookupService}), which today is an in-process call but will
 * become an inter-service RPC in the future.
 *
 * <p>The inbox insert and all per-target {@code webhook_event} inserts run in
 * the same transaction. If any step fails, the inbox insert rolls back with
 * everything else, and the next Kafka redelivery is free to retry cleanly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookIngestionService {

    private final WebhookEventRepository webhookEventRepository;
    private final InboxEventRepository inboxEventRepository;
    private final MerchantLookupService merchantWebhookApi;
    private final ObjectMapper objectMapper;
    private final SignerUtil signerUtil;

    static final String CONSUMER_ID = "webhook-consumer";

    /**
     * @return ids of newly-created {@link WebhookEvent} rows for downstream
     *         Redis enqueue by the caller. Empty on duplicate delivery or when
     *         the merchant has no active webhook configs for the event type.
     * @throws IllegalArgumentException if the envelope is malformed
     *         (routed to the DLQ by the consumer)
     */
    @Transactional
    public List<UUID> ingest(Map<String, Object> envelope) {
        UUID eventId = extractEventId(envelope);

        int inserted = inboxEventRepository.tryInsert(UUID.randomUUID(), eventId, CONSUMER_ID, LocalDateTime.now());

        if (inserted == 0) {
            log.info("Kafka event {} already consumed by {}, skipping fan-out", eventId, CONSUMER_ID);
            return List.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) envelope.get("data");

        if (data == null) {
            throw new IllegalArgumentException("Envelope missing 'data' section for event " + eventId);
        }

        String eventType = (String) data.get("eventType");
        Object merchantIdRaw = data.get("merchantId");

        if (merchantIdRaw == null) {
            log.warn("Envelope for event {} has no merchantId, skipping fan-out", eventId);
            return List.of();
        }

        UUID merchantId = UUID.fromString(merchantIdRaw.toString());

        List<WebhookTarget> targets = merchantWebhookApi.getActiveConfigsForEvent(merchantId, eventType);

        if (targets.isEmpty()) {
            log.debug("No webhook targets for merchant {} and event type {}", merchantId, eventType);
            return List.of();
        }

        String signatureJson;

        try {
            signatureJson = objectMapper.writeValueAsString(Map.of("event", eventType, "payload", data));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize webhook signature payload for event " + eventId, e);
        }

        List<UUID> savedIds = new ArrayList<>(targets.size());

        for (WebhookTarget target : targets) {
            String signature = signerUtil.sign(signatureJson, target.webhookSecret());

            WebhookEvent saved = webhookEventRepository.save(WebhookEvent.builder()
                    .merchantId(merchantId)
                    .eventType(eventType)
                    .payload(data)
                    .targetUrl(target.targetUrl())
                    .signature(signature)
                    .status(WebhookEventStatus.PENDING)
                    .nextRetryAt(LocalDateTime.now())
                    .build());

            savedIds.add(saved.getId());
        }

        log.info("Fanned out Kafka event {} to {} webhook target(s) for merchant {}", eventId, savedIds.size(), merchantId);
        return savedIds;
    }

    private UUID extractEventId(Map<String, Object> envelope) {
        Object raw = envelope.get("eventId");

        if (raw == null) {
            throw new IllegalArgumentException("Kafka envelope missing 'eventId', cannot dedupe");
        }

        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Kafka envelope 'eventId' is not a valid UUID: " + raw, e);
        }
    }
}
