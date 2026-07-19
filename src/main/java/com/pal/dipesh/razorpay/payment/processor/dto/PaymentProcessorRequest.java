package com.pal.dipesh.razorpay.payment.processor.dto;

import com.pal.dipesh.razorpay.common.entity.Money;
import com.pal.dipesh.razorpay.common.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public record PaymentProcessorRequest(
        UUID paymentId,
        PaymentMethod method,
        Money amount,
        String pan,
        String expiry,
        Map<String, Object> methodDetails
) {

    public static PaymentProcessorRequest card(UUID paymentId, Money amount, String pan, String expiry, Map<String, Object> methodDetails) {
        return new PaymentProcessorRequest(paymentId, PaymentMethod.CARD, amount, pan, expiry, methodDetails);
    }

    public static PaymentProcessorRequest nonCard(UUID paymentId, PaymentMethod method, Money amount, Map<String, Object> methodDetails) {
        return new PaymentProcessorRequest(paymentId, method, amount, null, null, methodDetails);
    }
}