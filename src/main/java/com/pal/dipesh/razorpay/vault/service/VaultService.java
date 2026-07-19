package com.pal.dipesh.razorpay.vault.service;

import com.pal.dipesh.razorpay.common.entity.Money;
import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorResponse;
import com.pal.dipesh.razorpay.vault.dto.request.TokenizeRequest;
import com.pal.dipesh.razorpay.vault.dto.response.TokenizeResponse;

import java.util.Map;
import java.util.UUID;

public interface VaultService {

    TokenizeResponse tokenize(TokenizeRequest tokenizeRequest, UUID merchantId);

    PaymentProcessorResponse charge(String token, UUID paymentId, Money amount, Map<String, Object> methodDetails);
}
