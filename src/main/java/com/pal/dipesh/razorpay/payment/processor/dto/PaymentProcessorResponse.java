package com.pal.dipesh.razorpay.payment.processor.dto;

public sealed interface PaymentProcessorResponse permits
        PaymentProcessorResponse.Success,
        PaymentProcessorResponse.Pending,
        PaymentProcessorResponse.Failure {

    record Success(String processorReference, String bankReference) implements PaymentProcessorResponse {
    }

    record Pending(String processorReference) implements PaymentProcessorResponse {
    }

    record Failure(String errorCode, String errorDescription) implements PaymentProcessorResponse {
    }
}