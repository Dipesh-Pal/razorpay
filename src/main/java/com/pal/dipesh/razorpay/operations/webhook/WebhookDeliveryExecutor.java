package com.pal.dipesh.razorpay.operations.webhook;

import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates a single webhook delivery attempt. Delegates all DB state
 * transitions to {@link WebhookAttemptService}. Deliberately performs the
 * merchant HTTP call outside any transaction so DB connections are never
 * held across network I/O.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryExecutor {

    private final WebhookAttemptService webhookAttemptService;
    private final WebhookRetryQueue webhookRetryQueue;
    private final RestClient webhookRestClient;

    @Value("${app.webhook.delivery.signature-header:X-Razorpay-Signature}")
    private String signatureHeader;

    public void deliver(UUID webhookEventId) {
        Optional<WebhookEvent> claimedOpt = webhookAttemptService.beginAttempt(webhookEventId);

        if (claimedOpt.isEmpty()) {
            log.debug("Webhook event {} not claimable (terminal or in-flight elsewhere), skipping delivery", webhookEventId);
            return;
        }

        WebhookEvent event = claimedOpt.get();

        try {
            var response = webhookRestClient.post()
                    .uri(event.getTargetUrl())
                    .header(signatureHeader, event.getSignature())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "event", event.getEventType(),
                            "payload", event.getPayload()
                    ))
                    .retrieve()
                    .toBodilessEntity();

            int statusCode = response.getStatusCode().value();

            if (response.getStatusCode().is2xxSuccessful()) {
                webhookAttemptService.recordDelivered(webhookEventId, statusCode);
                log.info("Successfully delivered webhook event {} with HTTP {}", webhookEventId, statusCode);
            } else {
                handleFailure(webhookEventId, "HTTP" + statusCode, statusCode);
            }
        } catch (RestClientException e) {
            log.error("RestClientException while delivering webhook event {}: {}", webhookEventId, e.getMessage(), e);
            handleFailure(webhookEventId, e.getMessage(), null);
        }
    }

    private void handleFailure(UUID id, String error, Integer statusCode) {
        Optional<LocalDateTime> nextRetryAt = webhookAttemptService.recordAttemptFailed(id, error, statusCode);
        nextRetryAt.ifPresent(t -> webhookRetryQueue.enqueueIfAbsent(id, t));
    }
}
