package com.pal.dipesh.razorpay.payment.service.impl;

import com.pal.dipesh.razorpay.common.enums.PaymentActor;
import com.pal.dipesh.razorpay.common.enums.PaymentEvent;
import com.pal.dipesh.razorpay.common.enums.PaymentStatus;
import com.pal.dipesh.razorpay.payment.entity.Payment;
import com.pal.dipesh.razorpay.payment.entity.PaymentTransitionLog;
import com.pal.dipesh.razorpay.payment.repository.PaymentTransitionLogRepository;
import com.pal.dipesh.razorpay.payment.service.PaymentTransitionService;
import com.pal.dipesh.razorpay.payment.service.statemachine.PaymentStateMachine;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentTransitionServiceImpl implements PaymentTransitionService {

    private final PaymentTransitionLogRepository paymentTransitionLogRepository;
    private final PaymentStateMachine paymentStateMachine;

    @Override
    @Transactional
    public PaymentStatus apply(Payment payment, PaymentEvent paymentEvent) {
        PaymentStatus nextState = paymentStateMachine.getNextState(payment.getStatus(), paymentEvent);

        PaymentTransitionLog log = PaymentTransitionLog.builder()
                .payment(payment)
                .fromStatus(payment.getStatus())
                .eventType(paymentEvent)
                .toStatus(nextState)
                .actor(PaymentActor.SYSTEM) // TODO: fetch merchant context from spring security
                .occurredAt(LocalDateTime.now())
                .build();

        payment.setStatus(nextState);

        paymentTransitionLogRepository.save(log);

        return nextState;
    }
}
