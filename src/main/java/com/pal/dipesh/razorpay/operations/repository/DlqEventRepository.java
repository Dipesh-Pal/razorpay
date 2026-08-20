package com.pal.dipesh.razorpay.operations.repository;

import com.pal.dipesh.razorpay.operations.entity.DlqEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DlqEventRepository extends JpaRepository<DlqEvent, UUID> {
}
