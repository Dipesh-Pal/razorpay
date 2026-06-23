package com.pal.dipesh.razorpay.payment.repository;

import com.pal.dipesh.razorpay.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<List<Payment>> findByOrderRecord_Id(UUID orderId);
}