package com.pal.dipesh.razorpay.operations.settlement;

import com.pal.dipesh.razorpay.common.pojo.Money;
import com.pal.dipesh.razorpay.operations.settlement.dto.BankTransferResult;

import java.util.UUID;

public interface BankTransferProcessor {
    BankTransferResult initiateBankTransfer(UUID settlementId, UUID merchantId, Money amount, String bankAccountNumber, String ifscCode);
}
