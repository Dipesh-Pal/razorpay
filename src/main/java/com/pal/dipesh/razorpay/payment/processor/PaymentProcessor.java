package com.pal.dipesh.razorpay.payment.processor;

import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorResponse;

public interface PaymentProcessor {

    PaymentProcessorResponse charge(PaymentProcessorRequest request);
}
