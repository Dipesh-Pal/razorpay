package com.pal.dipesh.razorpay.operations.webhook;

import com.pal.dipesh.razorpay.operations.repository.InboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Deletes {@code inbox_event} rows older than the configured retention window.
 * Once no outbox message that old can still be replayed by Kafka, the inbox
 * row has no dedupe value and only costs storage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboxCleanupScheduler {

    private final InboxEventRepository inboxEventRepository;

    @Value("${app.webhook.inbox.retention-days:30}")
    private int retentionDays;

    @Transactional
    @Scheduled(cron = "${app.webhook.inbox.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        long deleted = inboxEventRepository.deleteByProcessedAtBefore(cutoff);

        if (deleted > 0) {
            log.info("Deleted {} inbox_event rows older than {}", deleted, cutoff);
        } else {
            log.debug("Inbox cleanup: no rows older than {}", cutoff);
        }
    }
}
