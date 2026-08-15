package com.pal.dipesh.razorpay.operations.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.pal.dipesh.razorpay.common.entity.WebhookTarget;
import com.pal.dipesh.razorpay.common.enums.WebhookEventStatus;
import com.pal.dipesh.razorpay.common.util.SignerUtil;
import com.pal.dipesh.razorpay.merchant.api.MerchantWebhookApi;
import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;
import com.pal.dipesh.razorpay.operations.repository.WebhookEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookKafkaConsumer {

    private final WebhookEventRepository webhookEventRepository;
    private final MerchantWebhookApi merchantWebhookApi;
    private final WebhookRetryQueue webhookRetryQueue;
    private final ObjectMapper objectMapper;
    private final SignerUtil signerUtil;

    @KafkaListener(topics = {
            "${app.kafka.topics.payment:payment.events}",
            "${app.kafka.topics.order:order.events}",
            "${app.kafka.topics.refund:refund.events}",
            "${app.kafka.topics.settlement:settlement.events}"
    })
    public void onWebhookEvent(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment ack) {
        try {
            Map<String, Object> envelope = record.value();
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            String eventType = (String) data.get("eventType");
            Object merchantIdRaw = data.get("merchantIdRaw");

            if (merchantIdRaw == null) {
                log.warn("No merchant id found was found, skipping the event: {}", eventType);
                ack.acknowledge();
                return;
            }

            UUID merchantId = UUID.fromString(merchantIdRaw.toString());
            List<WebhookTarget> targets = merchantWebhookApi.getActiveConfigsForEvent(merchantId, eventType);

            if (targets.isEmpty()) {
                log.debug("No webhook target was found, skipping the event: {}", eventType);
                ack.acknowledge();
                return;
            }

            Map<String, Object> signatureData = Map.of("event", eventType, "payload", data);
            String signatureJson = objectMapper.writeValueAsString(signatureData);

            for (WebhookTarget target : targets) {
                String signature = signerUtil.sign(signatureJson, target.webhookSecret());

                WebhookEvent webhookEvent = WebhookEvent.builder()
                        .merchantId(merchantId)
                        .eventType(eventType)
                        .payload(data)
                        .targetUrl(target.targetUrl())
                        .signature(signature)
                        .status(WebhookEventStatus.PENDING)
                        .nextRetryAt(LocalDateTime.now())
                        .build();

                webhookEvent = webhookEventRepository.save(webhookEvent);

                webhookRetryQueue.enqueue(webhookEvent.getId(), webhookEvent.getNextRetryAt());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Webhook consumer failed to process the record, offset: {}", record.offset(), e);

            // TODO: check exception for acknowledging
        }
    }
}
