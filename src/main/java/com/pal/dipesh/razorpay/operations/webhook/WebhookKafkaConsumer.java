package com.pal.dipesh.razorpay.operations.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin Kafka listener that delegates envelope handling to
 * {@link WebhookIngestionService} (transactional inbox + fan-out) and then
 * enqueues newly-created webhook events into Redis for delivery.
 *
 * <p>Ack semantics require {@code spring.kafka.listener.ack-mode=MANUAL}
 * and {@code enable-auto-commit=false}. On DB failure the record is rethrown
 * to Spring's {@code DefaultErrorHandler} for retry; on logic errors it is
 * recorded to the DLQ and acknowledged so Kafka moves past the offset.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookKafkaConsumer {

    private final WebhookIngestionService webhookIngestionService;
    private final WebhookDlqRecorder webhookDlqRecorder;
    private final WebhookRetryQueue webhookRetryQueue;

    @KafkaListener(topics = {
            "${app.kafka.topics.payment:payment.events}",
            "${app.kafka.topics.order:order.events}",
            "${app.kafka.topics.refund:refund.events}",
            "${app.kafka.topics.settlement:settlement.events}"
    })
    public void onWebhookEvent(ConsumerRecord<String, Map<String, Object>> record, Acknowledgment ack) {
        try {
            List<UUID> savedIds = webhookIngestionService.ingest(record.value());

            LocalDateTime now = LocalDateTime.now();

            for (UUID id : savedIds) {
                try {
                    webhookRetryQueue.enqueueIfAbsent(id, now);
                } catch (DataAccessException redisDown) {
                    log.warn("Redis enqueue failed for webhook event {}, reconciler will backfill: {}", id, redisDown.getMessage());
                }
            }

            ack.acknowledge();
        } catch (DataAccessException | CannotCreateTransactionException e) {
            log.error("Webhook consumer failed due to DB down, offset: {}", record.offset(), e);
            throw e;
        } catch (Exception logicError) {
            log.error("Webhook consumer failed due to logic error, offset: {}", record.offset(), logicError);
            webhookDlqRecorder.recordConsumerFailed(record, logicError.getMessage());
            ack.acknowledge();
        }
    }
}
