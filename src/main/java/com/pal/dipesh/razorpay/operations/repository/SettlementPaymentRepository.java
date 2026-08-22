package com.pal.dipesh.razorpay.operations.repository;

import com.pal.dipesh.razorpay.operations.entity.SettlementPayment;
import com.pal.dipesh.razorpay.operations.entity.SettlementPaymentId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, SettlementPaymentId> {
}
