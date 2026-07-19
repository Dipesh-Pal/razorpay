package com.pal.dipesh.razorpay.vault.entity;

import com.pal.dipesh.razorpay.common.enums.CardBrand;

import jakarta.persistence.*;

import lombok.*;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Encrypted card material in the vault.
 *
 * <p>Intentionally does <strong>not</strong> extend {@code BaseAuditEntity}.
 * A vault card is effectively write-once: the encrypted PAN/DEK never change
 * after insert; the only mutation is soft-deletion via {@link #deletedAt}.
 * A generic {@code updatedAt} would tick exactly once and would be strictly
 * redundant with {@code deletedAt}.
 *
 * <p>{@code createdAt} / {@code createdBy} are mandatory for PCI-DSS Req 10
 * (audit trail for cardholder-data access). {@link #deletedBy} is paired with
 * {@link #deletedAt} so every soft-deletion has a known principal.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "vault_card",
        indexes = {
                @Index(name = "idx_vault_card_bin_last_four", columnList = "bin, last_four")
        }
)
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class VaultCard {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "bin", nullable = false, length = 6)
    private String bin;

    @Column(name = "encrypted_pan", nullable = false)
    private byte[] encryptedPan;

    @Column(name = "encrypted_dek", nullable = false)
    private byte[] encryptedDek;

    @Column(name = "brand", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private CardBrand brand;

    @Column(name = "card_holder_name", nullable = false, length = 200)
    private String cardHolderName;

    @Column(name = "expiry_month", nullable = false, length = 2)
    private String expiryMonth;

    @Column(name = "expiry_year", nullable = false, length = 4)
    private String expiryYear;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
}