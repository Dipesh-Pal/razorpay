package com.pal.dipesh.razorpay.payment.gateway.dto;

import com.pal.dipesh.razorpay.common.entity.Money;
import com.pal.dipesh.razorpay.common.enums.PaymentMethod;

import java.util.Map;
import java.util.UUID;

public record PaymentRequest(
        UUID paymentId,
        UUID orderId,
        UUID merchantId,
        Money amount,
        PaymentMethod method,
        Map<String, Object> methodDetails
) {
}