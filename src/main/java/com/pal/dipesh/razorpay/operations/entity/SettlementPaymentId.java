package com.pal.dipesh.razorpay.operations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for {@link SettlementPayment}.
 *
 * <p>JPA requires composite-key classes to:
 * <ul>
 *   <li>implement {@link Serializable},</li>
 *   <li>override {@link Object#equals(Object)} and {@link Object#hashCode()}
 *       based on <em>all</em> key components,</li>
 *   <li>expose a public no-arg constructor.</li>
 * </ul>
 * Lombok generates the latter two; {@link Serializable} is implemented explicitly.
 *
 * <p>This class carries no audit fields — a composite key never does.
 */
@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SettlementPaymentId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "settlement_id", nullable = false, updatable = false)
    private UUID settlementId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;
}