package com.pal.dipesh.razorpay.merchant.service.impl;

import com.pal.dipesh.razorpay.common.exception.ApiKeyDisabledException;
import com.pal.dipesh.razorpay.common.exception.ResourceNotFoundException;
import com.pal.dipesh.razorpay.common.util.RandomizerUtil;
import com.pal.dipesh.razorpay.merchant.dto.request.ApiKeyCreateRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.pal.dipesh.razorpay.merchant.dto.response.ApiKeyResponse;
import com.pal.dipesh.razorpay.merchant.entity.ApiKey;
import com.pal.dipesh.razorpay.merchant.entity.Merchant;
import com.pal.dipesh.razorpay.merchant.mapper.ApiKeyMapper;
import com.pal.dipesh.razorpay.merchant.repository.ApiKeyRepository;
import com.pal.dipesh.razorpay.merchant.repository.MerchantRepository;
import com.pal.dipesh.razorpay.merchant.service.ApiKeyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    @Override
    @Transactional
    public ApiKeyCreateResponse createApiKey(UUID merchantId, ApiKeyCreateRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(() -> new ResourceNotFoundException("merchant", merchantId));

        String apiKeyId = "rzp_" + request.environment().name().toLowerCase() + "_" + RandomizerUtil.randomBase64(24);
        String rawSecret = RandomizerUtil.randomBase64(48);

        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyId(apiKeyId)
                .keySecretHash(rawSecret) // TODO: encrypt using Bcrypt
                .environment(request.environment())
                .build();

        apiKey = apiKeyRepository.save(apiKey);

        return apiKeyMapper.toApiKeyCreateResponse(apiKey, rawSecret);
    }

    @Override
    public List<ApiKeyResponse> listByMerchantId(UUID merchantId) {
        return apiKeyMapper.toApiKeyResponse(
                apiKeyRepository
                        .findByMerchant_Id(merchantId)
                        .orElseThrow(() -> new ResourceNotFoundException("merchant", merchantId))
        );
    }

    @Override
    @Transactional
    public void revoke(UUID merchantId, String keyId) {
//        ApiKey apiKey = apiKeyRepository.findByKeyId(keyId)
//                .filter(k -> k.getMerchant().getId().equals(merchantId))
//                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        ApiKey apiKey = apiKeyRepository.findByKeyIdAndMerchant_Id(keyId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        apiKey.setEnabled(false);
        apiKey.setRevokedAt(LocalDateTime.now());
        apiKey.setRevokedBy("system"); // TODO: set actual user who revoked the key

        apiKeyRepository.save(apiKey);
    }

    @Override
    @Transactional
    public ApiKeyCreateResponse rotateKey(UUID merchantId, String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndMerchant_Id(keyId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        if(!apiKey.isEnabled()){
           throw new ApiKeyDisabledException("API_KEY_DISABLED", "API key with id " + keyId + " is disabled and cannot be rotated");
        }

        String newRawSecret = RandomizerUtil.randomBase64(40);
        apiKey.setPreviousKeySecretHash(apiKey.getKeySecretHash());
        apiKey.setKeySecretHash(newRawSecret); // TODO: encrypt using Bcrypt
        apiKey.setRotatedAt(LocalDateTime.now());
        apiKey.setRotatedBy("system"); // TODO: set actual user who rotated the key
        apiKey.setGracePeriodExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hours grace period for old key to work

        apiKeyRepository.save(apiKey);

        return apiKeyMapper.toApiKeyCreateResponse(apiKey, newRawSecret);
    }
}