package com.pal.dipesh.razorpay.merchant.cache;

import java.util.Optional;

public interface ApiKeyCache {
    Optional<ApiKeyCacheEntry> get(String keyId);

    void put(String keyId, ApiKeyCacheEntry entry);

    void evict(String keyId);
}
