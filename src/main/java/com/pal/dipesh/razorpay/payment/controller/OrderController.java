package com.pal.dipesh.razorpay.payment.controller;

import com.pal.dipesh.razorpay.payment.dto.request.OrderCreateRequest;
import com.pal.dipesh.razorpay.payment.dto.response.OrderResponse;
import com.pal.dipesh.razorpay.payment.service.OrderService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    UUID merchantId = UUID.fromString("b8ff9c28-3114-4296-a406-8f8a5d42661b"); // TODO: Replace with MerchantContext

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(merchantId, request));
    }
}