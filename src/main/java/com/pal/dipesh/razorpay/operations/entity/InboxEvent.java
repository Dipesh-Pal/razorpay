package com.pal.dipesh.razorpay.operations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records that a Kafka event has been consumed by a specific consumer.
 * Provides idempotency against at-least-once Kafka delivery: a consumer's
 * first action on a new record is to insert a row here; if the
 * {@code (event_id, consumer_id)} composite already exists (enforced by
 * {@code idx_inbox_event_dedupe}), the record is a redelivery and downstream
 * fan-out is skipped.
 *
 * <p>Rows are immutable once written. {@code processed_at} is the wall-clock
 * time of first consumption. Old rows are pruned by
 * {@link com.pal.dipesh.razorpay.operations.webhook.InboxCleanupScheduler}.
 *
 * <p>Intentionally does <strong>not</strong> extend {@code BaseAuditEntity}:
 * {@code processed_at} <em>is</em> the creation timestamp and no other actor
 * information is meaningful for an inbox record.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(
        name = "inbox_event",
        indexes = {
                @Index(name = "idx_inbox_event_dedupe", columnList = "event_id, consumer_id", unique = true)
        }
)
public class InboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "consumer_id", nullable = false, updatable = false, length = 64)
    private String consumerId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
}
