package com.pal.dipesh.razorpay.operations.webhook;

import com.pal.dipesh.razorpay.common.enums.WebhookEventStatus;
import com.pal.dipesh.razorpay.common.exception.ResourceNotFoundException;
import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;
import com.pal.dipesh.razorpay.operations.repository.WebhookEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns all DB state transitions for a single webhook delivery attempt:
 * claim (before HTTP), success (after HTTP 2xx), and failure (after HTTP non-2xx
 * or exception, including exponential backoff and DLQ routing).
 *
 * <p>Each public method is a short transaction. The webhook's HTTP call is
 * deliberately performed outside these transactions by {@link WebhookDeliveryExecutor}
 * so DB connections are never held across network I/O.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookAttemptService {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDlqRecorder webhookDlqRecorder;

    private static final int MAX_ATTEMPTS = 7;

    private static final List<Duration> BACKOFF = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(8),
            Duration.ofHours(24)
    );

    /**
     * Atomically claims the event for this attempt via a CAS UPDATE on status.
     * Returns the loaded event if the claim succeeded, or empty if another executor
     * already owns the event or if it is terminal.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<WebhookEvent> beginAttempt(UUID id) {
        int claimed = webhookEventRepository.claimForAttempt(id, LocalDateTime.now());

        if (claimed == 0) {
            return Optional.empty();
        }

        return webhookEventRepository.findById(id);
    }

    /**
     * Records a successful (2xx) delivery outcome. Called after the HTTP call
     * returns success, from outside any transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDelivered(UUID id, int statusCode) {
        WebhookEvent event = webhookEventRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", id));
        event.setStatus(WebhookEventStatus.DELIVERED);
        event.setLastResponseCode(statusCode);
        event.setDeliveredAt(LocalDateTime.now());

        webhookEventRepository.save(event);
    }

    /**
     * Records a failed attempt. If the retry budget is exhausted the event moves
     * to DEAD and a DLQ row is written atomically in the same transaction.
     * Otherwise the event moves to FAILED with next_retry_at computed from the
     * exponential backoff schedule.
     *
     * @return the next retry time if the event was rescheduled, or empty if it
     *         was moved to DEAD/DLQ. The caller is responsible for enqueuing the
     *         retry into Redis if a value is returned.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<LocalDateTime> recordAttemptFailed(UUID id, String error, Integer statusCode) {
        WebhookEvent event = webhookEventRepository.findById(id).orElseThrow(()  -> new ResourceNotFoundException("WebhookEvent", id));
        event.setLastResponseBody(error);

        if (statusCode != null) {
            event.setLastResponseCode(statusCode);
        }

        int attempts = event.getAttempts();

        if (attempts >= MAX_ATTEMPTS) {
            event.setStatus(WebhookEventStatus.DEAD);
            webhookDlqRecorder.recordAfterAttemptExhausted(event, error);
            log.warn("Webhook event {} exhausted retry budget after {} attempts, moved to DEAD/DLQ", id, attempts);
            return Optional.empty();
        }

        LocalDateTime nextRetryAt = LocalDateTime.now().plus(computeBackoff(attempts));
        event.setStatus(WebhookEventStatus.FAILED);
        event.setNextRetryAt(nextRetryAt);

        webhookEventRepository.save(event);

        log.info("Webhook event {} scheduled for retry at {} (attempt {} of {})", id, nextRetryAt, attempts, MAX_ATTEMPTS);
        return Optional.of(nextRetryAt);
    }

    private Duration computeBackoff(int attempts) {
        int idx = Math.min(attempts - 1, BACKOFF.size() - 1);
        return BACKOFF.get(idx);
    }
}
