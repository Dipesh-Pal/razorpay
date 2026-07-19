package com.pal.dipesh.razorpay.vault.controller;

import com.pal.dipesh.razorpay.common.annotation.ResponseMessage;
import com.pal.dipesh.razorpay.merchant.security.MerchantContext;
import com.pal.dipesh.razorpay.vault.dto.request.TokenizeRequest;
import com.pal.dipesh.razorpay.vault.dto.response.TokenizeResponse;
import com.pal.dipesh.razorpay.vault.service.VaultService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vault")
public class VaultController {

    private final MerchantContext merchantContext;
    private final VaultService vaultService;

    @PostMapping
    @ResponseMessage("Card tokenized")
    public ResponseEntity<TokenizeResponse> tokenize(@Valid @RequestBody TokenizeRequest tokenizeRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaultService.tokenize(tokenizeRequest, merchantContext.getMerchantId()));
    }
}
