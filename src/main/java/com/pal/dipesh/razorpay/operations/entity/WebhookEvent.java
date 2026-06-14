package com.pal.dipesh.razorpay.operations.entity;

import com.pal.dipesh.razorpay.common.enums.WebhookEventStatus;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound webhook delivery aggregate.
 *
 * <p>Intentionally does <strong>not</strong> extend {@code BaseAuditEntity}:
 * this row is genuinely mutated on every delivery attempt (retry counter,
 * {@link #lastAttemptAt}, {@link #lastResponseCode} ...). A generic
 * {@code updatedAt} would be churned on every retry and would always resolve
 * to {@code "system"} — {@link #lastAttemptAt} already captures last-touched
 * with more precision.
 *
 * <p>Only {@code createdAt} is auditored (via Spring Data JPA Auditing) — it
 * anchors retention, retry-budget, and "oldest pending webhook" lag metrics.
 * No {@code createdBy} either: the row is always created by the payment
 * state machine.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "webhook_event")
@EntityListeners(AuditingEntityListener.class)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Column(name = "signature", nullable = false)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookEventStatus status;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_response_code")
    private Integer lastResponseCode;

    @Column(name = "last_response_body", length = 1000)
    private String lastResponseBody;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}