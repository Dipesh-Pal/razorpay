package com.pal.dipesh.razorpay.operations.entity;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Dead-letter row for a {@link WebhookEvent} whose retry budget was
 * exhausted. Either replayed by an operator (sets {@link #replayedAt} /
 * {@link #replayedBy}) or kept indefinitely for forensic review.
 *
 * <p>Intentionally does <strong>not</strong> extend {@code BaseAuditEntity}:
 * {@link #movedAt} <em>is</em> the creation timestamp (the moment retries
 * were exhausted) — a separate {@code createdAt} would be redundant.
 * {@link #replayedBy} is paired with {@link #replayedAt} because a replay
 * is a deliberate human action whose actor matters for incident review.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dlq_event")
@EqualsAndHashCode(of = "id")
public class DlqEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_event_id", nullable = false, updatable = false)
    private WebhookEvent webhookEvent;

    @Column (name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "final_error", length = 1000)
    private String finalError;

    @Column(name = "moved_at")
    private LocalDateTime movedAt;

    @Column(name = "replayed_at")
    private LocalDateTime replayedAt;

    @Column(name = "replayed_by", length = 100)
    private String replayedBy;
}