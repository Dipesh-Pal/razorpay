package com.pal.dipesh.razorpay.operations.entity;

import com.pal.dipesh.razorpay.payment.entity.Payment;

import jakarta.persistence.*;

import lombok.*;

/**
 * Many-to-many join row between a {@link Settlement} and a {@link Payment}:
 * "this payment was included in this settlement batch".
 *
 * <p>Carries no audit fields — a join table never has authors, and the parent
 * {@link Settlement#getCreatedAt()} already records when the inclusion
 * happened (all rows in a given settlement are written in the same batch).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(
        name = "settlement_payment",
        indexes = {
                @Index(name = "idx_settlement_payment_payment_id", columnList = "payment_id")
        }
)
public class SettlementPayment {

    @EmbeddedId
    private SettlementPaymentId id;

    @MapsId("settlementId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false, updatable = false)
    private Settlement settlement;
}