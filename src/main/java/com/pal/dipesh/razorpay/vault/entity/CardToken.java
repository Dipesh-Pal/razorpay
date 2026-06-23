package com.pal.dipesh.razorpay.vault.entity;

import jakarta.persistence.*;

import lombok.*;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Merchant-facing token that references a {@link VaultCard}.
 *
 * <p>Intentionally does <strong>not</strong> extend {@code BaseAuditEntity}.
 * A token's lifecycle is {@code ACTIVE -> REVOKED} (captured by
 * {@link #revokedAt}); the row is otherwise immutable. A generic
 * {@code updatedAt} would tick exactly once and adds no information beyond
 * {@code revokedAt}.
 *
 * <p>{@code createdAt} / {@code createdBy} record the issuing API call.
 * {@link #revokedBy} distinguishes customer-initiated revocation (e.g. card
 * removed from saved methods) from merchant-initiated or system-initiated
 * revocation — each has different incident-response implications.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "card_token",
        indexes = {
                @Index(name = "idx_card_token_vault_card_id", columnList = "vault_card_id"),
                @Index(name = "idx_card_token_customer_id", columnList = "customer_id"),
                @Index(name = "idx_card_token_merchant_id", columnList = "merchant_id")
        }
)
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class CardToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vault_card_id", nullable = false)
    private VaultCard vaultCard;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "token", nullable = false, unique = true, length = 50)
    private String token;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private String revokedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
}