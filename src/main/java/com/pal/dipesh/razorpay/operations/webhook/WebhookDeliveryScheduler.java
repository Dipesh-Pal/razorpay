package com.pal.dipesh.razorpay.operations.webhook;

import com.pal.dipesh.razorpay.common.enums.WebhookEventStatus;
import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;
import com.pal.dipesh.razorpay.operations.repository.WebhookEventRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryScheduler {

    private final WebhookDeliveryExecutor webhookDeliveryExecutor;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookRetryQueue webhookRetryQueue;

    private ExecutorService virtualThreadExecutor;

    @PostConstruct
    public void init() {
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    public void shutdown() {
        if (virtualThreadExecutor != null) {
            virtualThreadExecutor.shutdown();
        }
    }

    @Value("${app.webhook.delivery.poll-batch-size:100}")
    private int batchSize = 100;

    @Value("${app.webhook.delivery.visibility-timeout:PT2M}")
    private Duration visibilityTimeout = Duration.ofMinutes(2);

    @Scheduled(fixedDelay = 1000)
    public void pollAndDeliver(){
        Set<UUID> dueEvents = webhookRetryQueue.pollDue(batchSize);

        if(dueEvents.isEmpty()){
            log.debug("No due webhook events found for delivery");
            return;
        }

        log.info("Found {} due webhook events for delivery", dueEvents.size());

        for(UUID webhookEventId : dueEvents){
            virtualThreadExecutor.submit(() -> {
                webhookDeliveryExecutor.deliver(webhookEventId);
            });
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void reconcileFromDatabase(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime visibilityCutoff = now.minus(visibilityTimeout);

        int reset = webhookEventRepository.resetStuckInProgress(visibilityCutoff);

        if (reset > 0) {
            log.warn("Reconciler reset {} stuck IN_PROGRESS webhook events to FAILED", reset);
        }

        List<WebhookEvent> due = webhookEventRepository.findDueForReconciliation(
                List.of(WebhookEventStatus.PENDING, WebhookEventStatus.FAILED),
                now,
                visibilityCutoff,
                PageRequest.of(0, batchSize, Sort.by("nextRetryAt").ascending()));

        if (due.isEmpty()) {
            log.debug("Reconciler found no orphaned webhook events");
            return;
        }

        log.info("Reconciler backfilling {} orphaned webhook events into retry queue", due.size());

        for(WebhookEvent webhookEvent : due){
            webhookRetryQueue.enqueueIfAbsent(webhookEvent.getId(), webhookEvent.getNextRetryAt());
        }
    }
}
