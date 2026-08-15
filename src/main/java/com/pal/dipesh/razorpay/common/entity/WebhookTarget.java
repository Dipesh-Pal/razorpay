package com.pal.dipesh.razorpay.common.entity;

import java.util.UUID;

public record WebhookTarget(
        UUID configId,
        String targetUrl,
        String webhookSecret
) {
}
