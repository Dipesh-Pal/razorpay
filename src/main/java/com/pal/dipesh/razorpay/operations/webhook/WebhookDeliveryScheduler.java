package com.pal.dipesh.razorpay.operations.webhook;

import com.pal.dipesh.razorpay.common.enums.WebhookEventStatus;
import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;
import com.pal.dipesh.razorpay.operations.repository.WebhookEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryScheduler {

    private final WebhookRetryQueue webhookRetryQueue;
    private final WebhookEventRepository webhookEventRepository;

    @Value("${app.webhook.delivery.poll-batch-size:100}")
    private int batchSize = 100;

    @Scheduled(fixedDelay = 1000)
    public void pollAndDeliver(){
        Set<UUID> dueEvents = webhookRetryQueue.pollDue(batchSize);

        if(dueEvents.isEmpty()){
            log.debug("No due webhook events found for delivery");
            return;
        }

        log.info("Found {} due webhook events for delivery", dueEvents.size());

        for(UUID webhookEventId : dueEvents){
            // executor.deliver(webhookEventId);
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void reconcileFromDatabase(){
        LocalDateTime now = LocalDateTime.now();
        List<WebhookEvent> due = webhookEventRepository.findByStatusAndNextRetryAtBefore(WebhookEventStatus.PENDING, now);

        for(WebhookEvent webhookEvent : due){
            webhookRetryQueue.enqueueIfAbsent(webhookEvent.getId(), webhookEvent.getNextRetryAt());
        }
    }
}
