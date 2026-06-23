package com.pal.dipesh.razorpay.merchant.repository;

import com.pal.dipesh.razorpay.merchant.entity.Merchant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    boolean existsByEmail(String email);
}