package com.pal.dipesh.razorpay.operations.repository;

import com.pal.dipesh.razorpay.common.enums.WebhookEventStatus;
import com.pal.dipesh.razorpay.operations.entity.WebhookEvent;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    @Query("""
            SELECT w FROM WebhookEvent w
            WHERE w.status IN :statuses
              AND w.nextRetryAt < :now
              AND (w.lastAttemptAt IS NULL OR w.lastAttemptAt < :visibilityCutoff)
            ORDER BY w.nextRetryAt ASC
            """)
    List<WebhookEvent> findDueForReconciliation(
            @Param("statuses") Collection<WebhookEventStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("visibilityCutoff") LocalDateTime visibilityCutoff,
            Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WebhookEvent w
            SET w.status = com.pal.dipesh.razorpay.common.enums.WebhookEventStatus.IN_PROGRESS,
                w.attempts = w.attempts + 1,
                w.lastAttemptAt = :now,
                w.version = w.version + 1
            WHERE w.id = :id
              AND w.status IN (com.pal.dipesh.razorpay.common.enums.WebhookEventStatus.PENDING,
                               com.pal.dipesh.razorpay.common.enums.WebhookEventStatus.FAILED)
            """)
    int claimForAttempt(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE WebhookEvent w
            SET w.status = com.pal.dipesh.razorpay.common.enums.WebhookEventStatus.FAILED,
                w.lastResponseBody = 'Reset by reconciler: stuck IN_PROGRESS',
                w.version = w.version + 1
            WHERE w.status = com.pal.dipesh.razorpay.common.enums.WebhookEventStatus.IN_PROGRESS
              AND w.lastAttemptAt < :visibilityCutoff
            """)
    int resetStuckInProgress(@Param("visibilityCutoff") LocalDateTime visibilityCutoff);
}
