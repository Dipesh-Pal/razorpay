package com.pal.dipesh.razorpay.operations.settlement;

import com.pal.dipesh.razorpay.merchant.api.MerchantLookupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEngine {

    private final MerchantLookupService merchantLookupService;
    private final SettlementTransactionExecutor settlementTransactionExecutor;

    @Scheduled(cron = "0 0 23 * * *") // Run every day at 11 PM
    public void runScheduled() {
        log.info("Running settlement engine at 11 PM");
        run();
    }

    public void run(){
        List<UUID> activeMerchantIds = merchantLookupService.getAllActiveMerchantIds();

        log.info("Processing settlements for {} active merchants", activeMerchantIds.size());

        try(ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()){
            List<Future<?>> futures = new ArrayList<>();

            for(UUID activeMerchantId : activeMerchantIds){
                futures.add(executorService.submit(() -> {
                    try{
                        settlementTransactionExecutor.processForMerchant(activeMerchantId, null);
                    } catch (Exception e){
                        log.error("Settlement processing failed for merchantId: {}", activeMerchantId, e);
                    }
                }));
            }

            for(Future<?> future : futures){
                try{
                    future.get();
                } catch (InterruptedException | ExecutionException e){
                    log.error("Settlement batch future failed", e);
                    throw new RuntimeException(e);
                }
            }
        }

        log.info("Settlement batch completed");
    }
}
