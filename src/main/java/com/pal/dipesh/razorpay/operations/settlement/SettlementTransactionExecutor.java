package com.pal.dipesh.razorpay.operations.settlement;

import com.pal.dipesh.razorpay.common.enums.EventAggregateType;
import com.pal.dipesh.razorpay.common.exception.ResourceNotFoundException;
import com.pal.dipesh.razorpay.common.pojo.Money;
import com.pal.dipesh.razorpay.common.enums.SettlementStatus;
import com.pal.dipesh.razorpay.common.pojo.SettlementBankingDetails;
import com.pal.dipesh.razorpay.merchant.api.MerchantLookupService;
import com.pal.dipesh.razorpay.operations.entity.Settlement;
import com.pal.dipesh.razorpay.operations.entity.SettlementPayment;
import com.pal.dipesh.razorpay.operations.entity.SettlementPaymentId;
import com.pal.dipesh.razorpay.operations.repository.SettlementPaymentRepository;
import com.pal.dipesh.razorpay.operations.repository.SettlementRepository;
import com.pal.dipesh.razorpay.operations.settlement.dto.BankTransferResult;
import com.pal.dipesh.razorpay.payment.api.PaymentLookupService;
import com.pal.dipesh.razorpay.payment.entity.Payment;
import com.pal.dipesh.razorpay.payment.outbox.OutboxEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTransactionExecutor {

    private static final double FEE_RATE = 0.02; // 2% fee
    private static final double GST_RATE = 0.18; // 18% GST

    private final SettlementPaymentRepository settlementPaymentRepository;
    private final BankTransferProcessor bankTransferProcessor;
    private final MerchantLookupService merchantLookupService;
    private final PaymentLookupService paymentLookupService;
    private final SettlementRepository settlementRepository;

    // TODO: Publisher inside it's own DB.
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void processForMerchant(UUID merchantId, LocalDate settlementDate) {
        List<Payment> unsettledPayments = paymentLookupService.findUnsettledCapturedPaymentsForMerchant(merchantId);

        log.info("Processing {} unsettled payments for merchantId: {} on {} date", unsettledPayments.size(), merchantId, settlementDate);

        if (unsettledPayments.isEmpty()) {
            log.info("No unsettled payments found for merchant {}", merchantId);
            return;
        }

        Money gross = unsettledPayments.stream()
                .map(Payment::getAmount)
                .reduce(Money::add)
                .orElseThrow();

        int fee = Math.toIntExact(Math.round(gross.getAmountUnits() * FEE_RATE));
        int gst = Math.toIntExact(Math.round(fee * GST_RATE));

        Money feeAmount = Money.of(fee, gross.getCurrency());
        Money gstAmount = Money.of(gst, gross.getCurrency());
        Money netAmount = gross.subtract(feeAmount).subtract(gstAmount);

        Settlement settlement = Settlement.builder()
                .merchantId(merchantId)
                .grossAmount(gross)
                .feeAmount(feeAmount)
                .gstAmount(gstAmount)
                .netAmount(netAmount)
                .status(SettlementStatus.INITIATED)
                .build();

        settlement = settlementRepository.save(settlement);

        try {
            List<SettlementPayment> links = new ArrayList<>();

            for(Payment payment: unsettledPayments){
                links.add(new SettlementPayment(
                        new SettlementPaymentId(settlement.getId(), payment.getId()),
                        settlement
                        )
                );
            }

            settlementPaymentRepository.saveAll(links);

            SettlementBankingDetails bankingDetails = merchantLookupService.getSettlementBankingDetails(merchantId);
            BankTransferResult bankTransferResult = bankTransferProcessor.initiateBankTransfer(settlement.getId(), merchantId, netAmount, bankingDetails.accountNumber(), bankingDetails.ifscCode());

            settlement.setStatus(SettlementStatus.TRANSFER_PENDING);
            settlement.setBankReference(bankTransferResult.registrationRef());

            settlementRepository.save(settlement);
        } catch (Exception e) {
            log.error("Settlement failed for settlementId: {} merchantId: {} on date: {}", settlement.getId(), merchantId, settlementDate, e);
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setFailedAt(LocalDateTime.now());
            settlementRepository.save(settlement);
        }
    }

    @Transactional
    public void resolveTransfer(UUID SettlementId, String errorCode, String errorDescription){
        Settlement settlement = settlementRepository.findById(SettlementId).orElseThrow(() -> new ResourceNotFoundException("Settlement", SettlementId));

        if(errorCode == null){
            settlement.setProcessedAt(LocalDateTime.now());
            settlement.setStatus(SettlementStatus.PROCESSED);

            log.info("Settlement processed successfully for settlementId: {} merchantId: {}", settlement.getId(), settlement.getMerchantId());

            settlementRepository.save(settlement);
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlement.getId(),
                    "SETTLEMENT_PROCESSED", Map.of(
                        "settlementId", settlement.getId().toString(),
                        "merchantId", settlement.getMerchantId().toString(),
                        "status", settlement.getStatus().name(),
                        "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                        "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));
        } else {
            settlement.setFailedAt(LocalDateTime.now());
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setFailureReason(errorCode + " : " + errorDescription);

            log.warn("Settlement failed for settlementId: {} merchantId: {} with errorCode: {} and errorDescription: {}", settlement.getId(), settlement.getMerchantId(), errorCode, errorDescription);

            settlementRepository.save(settlement);
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlement.getId(),
                    "SETTLEMENT_FAILED", Map.of(
                            "settlementId", settlement.getId().toString(),
                            "merchantId", settlement.getMerchantId().toString(),
                            "status", settlement.getStatus().name(),
                            "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                            "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));
        }
    }
}
