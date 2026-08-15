package com.pal.dipesh.razorpay.merchant.api;

import com.pal.dipesh.razorpay.common.entity.WebhookTarget;

import java.util.List;
import java.util.UUID;

public interface MerchantWebhookApi {
    List<WebhookTarget> getActiveConfigsForEvent(UUID merchantId, String eventType);
}
