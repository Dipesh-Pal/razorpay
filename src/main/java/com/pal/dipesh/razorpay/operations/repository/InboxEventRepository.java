package com.pal.dipesh.razorpay.operations.repository;

import com.pal.dipesh.razorpay.operations.entity.InboxEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    /**
     * Atomically inserts an inbox row if a row with the same
     * {@code (event_id, consumer_id)} does not already exist. Returns 1 if the
     * row was newly inserted, 0 if the composite unique index
     * ({@code idx_inbox_event_dedupe}) already had a matching row.
     *
     * <p><strong>Why native SQL:</strong> JPA/JPQL does not define a single-row
     * {@code INSERT ... VALUES} statement (the spec only covers
     * {@code INSERT INTO ... SELECT}), and there is no portable JPQL equivalent
     * of {@code ON CONFLICT DO NOTHING}. Any check-then-{@code save} alternative
     * (in JPQL or plain Java) either races between concurrent consumers or
     * marks the surrounding JPA transaction as rollback-only on constraint
     * violation, preventing the caller's fan-out from committing.
     *
     * <p><strong>Why the conflict target is explicit:</strong> {@code ON CONFLICT
     * (event_id, consumer_id)} targets only the dedupe index; a hypothetical
     * primary-key collision on {@code id} would still surface as an error
     * instead of being silently swallowed.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO inbox_event (id, event_id, consumer_id, processed_at)
            VALUES (:id, :eventId, :consumerId, :processedAt)
            ON CONFLICT (event_id, consumer_id) DO NOTHING
            """, nativeQuery = true)
    int tryInsert(@Param("id") UUID id,
                  @Param("eventId") UUID eventId,
                  @Param("consumerId") String consumerId,
                  @Param("processedAt") LocalDateTime processedAt);

    long deleteByProcessedAtBefore(LocalDateTime cutoff);
}
