package com.pal.dipesh.razorpay.operations.webhook;

import com.pal.dipesh.razorpay.operations.entity.DlqEvent;
import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;
import com.pal.dipesh.razorpay.operations.repository.DlqEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDlqRecorder {

    private final DlqEventRepository dlqEventRepository;

    /**
     * Writes a DLQ row for a webhook whose retry budget has been exhausted.
     * Participates in the caller's transaction (typically
     * {@link WebhookAttemptService#recordAttemptFailed}) so that the parent
     * webhook_event's status transition to DEAD and the dlq_event insert
     * either both commit or both roll back.
     *
     * <p>Does <strong>not</strong> mutate {@code webhookEvent}'s status here:
     * the caller sets {@code status = DEAD} on the managed pojo and Hibernate
     * flushes it at commit alongside this insert.
     */
    @Transactional
    public void recordAfterAttemptExhausted(WebhookEvent webhookEvent, String finalError) {
        log.info("Recording webhook event {} in DLQ due to final error: {}", webhookEvent.getId(), finalError);

        DlqEvent dlqEvent = DlqEvent.builder()
                .webhookEvent(webhookEvent)
                .merchantId(webhookEvent.getMerchantId())
                .finalError(finalError)
                .payload(webhookEvent.getPayload())
                .movedAt(LocalDateTime.now())
                .build();

        dlqEventRepository.save(dlqEvent);
    }

    /**
     * Writes a DLQ row for a Kafka message that could not be processed by the
     * consumer (poison payload, logic error, etc.). Runs in its own transaction:
     * this method is invoked from the Kafka listener where there is no ambient
     * business transaction, and the DLQ write must succeed independently of any
     * failure that led us here.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConsumerFailed(ConsumerRecord<String, Map<String, Object>> record, String error) {
        log.info("Recording consumer failure in DLQ for record with key {} due to error: {}", record.key(), error);

        Map<String, Object> envelope = record.value();
        UUID merchantId = null;

        try {
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            Object merchantIdRaw = data != null ? data.get("merchantId") : null;

            if (merchantIdRaw != null) {
                merchantId = UUID.fromString(merchantIdRaw.toString());
            }
        } catch (Exception ignored) { }

        DlqEvent dlqEvent = DlqEvent.builder()
                .webhookEvent(null)
                .merchantId(merchantId)
                .finalError(error)
                .payload(envelope != null ? envelope : Map.of())
                .movedAt(LocalDateTime.now())
                .build();

        dlqEventRepository.save(dlqEvent);
    }
}
