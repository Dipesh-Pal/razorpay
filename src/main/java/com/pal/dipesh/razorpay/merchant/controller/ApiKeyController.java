package com.pal.dipesh.razorpay.merchant.controller;

import com.pal.dipesh.razorpay.common.annotation.ResponseMessage;
import com.pal.dipesh.razorpay.merchant.dto.request.ApiKeyCreateRequest;
import com.pal.dipesh.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.pal.dipesh.razorpay.merchant.dto.response.ApiKeyResponse;
import com.pal.dipesh.razorpay.merchant.security.AppUserContext;
import com.pal.dipesh.razorpay.merchant.service.ApiKeyService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchants/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final AppUserContext appUserContext;

    @PostMapping
    @ResponseMessage("API key generated")
    public ResponseEntity<ApiKeyCreateResponse> generateApiKey(@Valid @RequestBody ApiKeyCreateRequest apiKeys) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiKeyService.createApiKey(appUserContext.getMerchantId(), apiKeys));
    }

    @GetMapping
    @ResponseMessage("API keys fetched")
    public ResponseEntity<List<ApiKeyResponse>> getAllApiKeys() {
        return ResponseEntity.ok(apiKeyService.listByMerchantId(appUserContext.getMerchantId()));
    }

    @DeleteMapping("/{keyId}")
    @ResponseMessage("API key revoked")
    public ResponseEntity<Void> revokeApiKey(@PathVariable("keyId") String keyId) {
        apiKeyService.revoke(appUserContext.getMerchantId(), keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{keyId}")
    @ResponseMessage("API key rotated")
    public ResponseEntity<ApiKeyCreateResponse> rotateApiKey(@PathVariable("keyId") String keyId) {
        return ResponseEntity.of(Optional.ofNullable(apiKeyService.rotateKey(appUserContext.getMerchantId(), keyId)));
    }
}