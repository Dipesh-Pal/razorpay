package com.pal.dipesh.razorpay.payment.dto.response;

import com.pal.dipesh.razorpay.common.entity.Money;
import com.pal.dipesh.razorpay.common.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID id,

        UUID merchantId,

        String receipt,

        Money amount,

        OrderStatus status,

        Integer attempts,

        Map<String, Object> notes,

        LocalDateTime createdAt,

        LocalDateTime expiresAt
) {
}
