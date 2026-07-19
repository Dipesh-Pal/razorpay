package com.pal.dipesh.razorpay.payment.gateway.adapter;

import com.pal.dipesh.razorpay.payment.gateway.PaymentAdapter;
import com.pal.dipesh.razorpay.payment.gateway.dto.PaymentRequest;
import com.pal.dipesh.razorpay.payment.gateway.dto.PaymentResult;
import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorResponse;
import com.pal.dipesh.razorpay.vault.service.VaultService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CardPaymentAdapter implements PaymentAdapter {

    private final VaultService vaultService;

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        String token = (String) request.methodDetails().get("token");

        PaymentProcessorResponse response = vaultService.charge(token, request.paymentId(), request.amount(), request.methodDetails());

        return switch (response) {
            case PaymentProcessorResponse.Failure failure -> new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());
            case PaymentProcessorResponse.Pending pending -> new PaymentResult.Pending(pending.processorReference());
            case PaymentProcessorResponse.Success success -> new PaymentResult.Success(success.processorReference(), success.bankReference());
        };
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        System.out.println("Capture card payment...");

        return null;
    }
}