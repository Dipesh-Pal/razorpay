package com.pal.dipesh.razorpay.operations.repository;

import com.pal.dipesh.razorpay.common.enums.SettlementStatus;
import com.pal.dipesh.razorpay.operations.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByStatus(SettlementStatus settlementStatus);
}
