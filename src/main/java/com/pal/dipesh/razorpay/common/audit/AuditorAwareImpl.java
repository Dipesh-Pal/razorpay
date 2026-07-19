package com.pal.dipesh.razorpay.common.audit;

import com.pal.dipesh.razorpay.merchant.security.AppUserContext;
import com.pal.dipesh.razorpay.merchant.security.MerchantContext;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM_AUDITOR = "system";

    private final MerchantContext merchantContext;
    private final AppUserContext appUserContext;

    @Override
    public Optional<String> getCurrentAuditor() {
        if (appUserContext.getUsername() != null) {
            return Optional.of(appUserContext.getUsername());
        } else if (merchantContext.getKeyId() != null) {
            return Optional.of(merchantContext.getKeyId());
        }

        return Optional.of(SYSTEM_AUDITOR);
    }
}
