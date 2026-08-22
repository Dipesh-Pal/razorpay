package com.pal.dipesh.razorpay.operations.settlement;

import com.pal.dipesh.razorpay.common.enums.SettlementStatus;
import com.pal.dipesh.razorpay.operations.entity.Settlement;
import com.pal.dipesh.razorpay.operations.repository.SettlementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankSettlementCallbackSimulator {

    private final SettlementRepository settlementRepository;
    private final SettlementTransactionExecutor settlementTransactionExecutor;

    @Scheduled(fixedDelayString = "5000")
    public void processCallbacks(){
        List<Settlement> settlements = settlementRepository.findByStatus(SettlementStatus.TRANSFER_PENDING);

        if(settlements.isEmpty()){
            log.info("No settlements found with status TRANSFER_PENDING");
            return;
        }

        for(Settlement settlement : settlements){
            simulateCallback(settlement);
        }
    }

    private void simulateCallback(Settlement settlement) {
        log.info("Simulating callback for settlementId: {}", settlement.getId());
        settlementTransactionExecutor.resolveTransfer(settlement.getId(), null, null);
    }
}
