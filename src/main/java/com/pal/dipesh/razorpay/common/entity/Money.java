package com.pal.dipesh.razorpay.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Immutable monetary value stored as integer minor units (e.g. paise for INR)
 * plus an ISO-4217 currency code. Designed as a JPA {@link Embeddable} value
 * object — instances are interchangeable iff both fields match.
 *
 * <p>Two equal {@code Money} instances must be {@link #equals(Object)},
 * therefore {@link EqualsAndHashCode} is generated from all fields (the
 * default). Without this, value-object semantics break (e.g. assertions,
 * use in {@link java.util.Set}, deduplication).
 *
 * <p>Use {@link #of(int, String)} or {@link #inr(int)} for construction;
 * the protected no-arg constructor exists only for JPA.
 */
@Getter
@Embeddable
@EqualsAndHashCode
public class Money {
    @Column(name = "amount_units", nullable = false)
    private int amountUnits;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected Money() {
    }

    private Money(int amountUnits, String currency) {
        this.amountUnits = amountUnits;
        this.currency = currency;
    }

    public static Money of(int amountUnits, String currency) {
        return new Money(amountUnits, currency);
    }

    public static Money inr(int amountUnits) {
        return new Money(amountUnits, "INR");
    }

    public Money add(Money money) {
        if (!currency.equals(money.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }

        return new Money(this.amountUnits + money.amountUnits, this.currency);
    }

    public Money subtract(Money money) {
        if (!currency.equals(money.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }

        return new Money(this.amountUnits - money.amountUnits, this.currency);
    }
}