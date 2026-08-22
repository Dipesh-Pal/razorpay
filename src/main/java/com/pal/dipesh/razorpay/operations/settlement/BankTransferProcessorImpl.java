package com.pal.dipesh.razorpay.operations.settlement;

import com.pal.dipesh.razorpay.common.pojo.Money;
import com.pal.dipesh.razorpay.common.util.RandomizerUtil;
import com.pal.dipesh.razorpay.operations.settlement.dto.BankTransferResult;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class BankTransferProcessorImpl implements BankTransferProcessor {
    @Override
    public BankTransferResult initiateBankTransfer(UUID settlementId, UUID merchantId, Money amount, String bankAccountNumber, String ifscCode) {
        // Call the bank's API to initiate the transfer and return the result

        String registrationRef = "TXN_" + RandomizerUtil.randomBase64(8);

        log.debug("Bank Transfer call completed for settlementId: {}, merchantId: {}, amount: {}, bankAccountNumber: {}, ifscCode: {}, registrationRef: {}",
                settlementId, merchantId, amount, bankAccountNumber, ifscCode, registrationRef);

        return new BankTransferResult(registrationRef);
    }
}
