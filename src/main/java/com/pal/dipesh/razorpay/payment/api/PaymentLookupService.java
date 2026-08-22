package com.pal.dipesh.razorpay.payment.api;

import com.pal.dipesh.razorpay.payment.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentLookupService {
    List<Payment> findUnsettledCapturedPaymentsForMerchant(UUID merchantId);
}
