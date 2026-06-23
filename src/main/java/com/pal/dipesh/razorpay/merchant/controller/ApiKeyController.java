package com.pal.dipesh.razorpay.merchant.controller;

import com.pal.dipesh.razorpay.merchant.dto.request.ApiKeyCreateRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.pal.dipesh.razorpay.merchant.dto.response.ApiKeyResponse;
import com.pal.dipesh.razorpay.merchant.service.ApiKeyService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchants/{merchantId}/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKeyCreateResponse> generateApiKey(@PathVariable("merchantId") UUID merchantId, @Valid @RequestBody ApiKeyCreateRequest apiKeys) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiKeyService.createApiKey(merchantId, apiKeys));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getAllApiKeys(@PathVariable("merchantId") UUID merchantId) {
        return ResponseEntity.ok(apiKeyService.listByMerchantId(merchantId));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable("merchantId") UUID merchantId, @PathVariable("keyId") String keyId) {
        apiKeyService.revoke(merchantId, keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{keyId}/rotate")
    public ResponseEntity<ApiKeyCreateResponse> rotateApiKey(@PathVariable("merchantId") UUID merchantId, @PathVariable("keyId") String keyId) {
        return ResponseEntity.of(Optional.ofNullable(apiKeyService.rotateKey(merchantId, keyId)));
    }
}