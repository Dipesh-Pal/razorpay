package com.pal.dipesh.razorpay.payment.processor.strategy;

import com.pal.dipesh.razorpay.common.util.RandomizerUtil;
import com.pal.dipesh.razorpay.payment.processor.PaymentProcessor;
import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.pal.dipesh.razorpay.payment.processor.dto.PaymentProcessorResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CardPaymentProcessor implements PaymentProcessor {

    public static final String PAN_CARD_DECLINED = "400000000000002";
    public static final String PAN_CARD_EXPIRED = "400000000000069";

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        // Call Third Party
        String pan = request.pan();

        if(PAN_CARD_DECLINED.equals(pan)) {
            log.warn("Card declined for paymentId: {}, pan: {}", request.paymentId(), pan);
            return new PaymentProcessorResponse.Failure("CARD_DECLINED", "Card was declined by the bank");
        }

        if(PAN_CARD_EXPIRED.equals(pan)) {
            log.warn("Card expired for paymentId: {}, pan: {}", request.paymentId(), pan);
            return new PaymentProcessorResponse.Failure("CARD_EXPIRED", "Card has expired");
        }

        String processorRef = "CARD_PROCESSOR_" + RandomizerUtil.randomBase64(16);

        return new PaymentProcessorResponse.Pending(processorRef);
    }
}