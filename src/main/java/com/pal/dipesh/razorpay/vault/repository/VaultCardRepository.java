package com.pal.dipesh.razorpay.vault.repository;

import com.pal.dipesh.razorpay.vault.entity.VaultCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VaultCardRepository extends JpaRepository<VaultCard, UUID> {
}
