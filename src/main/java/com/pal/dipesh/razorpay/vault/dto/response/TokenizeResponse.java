package com.pal.dipesh.razorpay.vault.dto.response;

import com.pal.dipesh.razorpay.common.enums.CardBrand;

public record TokenizeResponse(
        String token,
        String lastFour,
        CardBrand brand,
        Integer expiryMonth,
        Integer expiryYear
) {
}
