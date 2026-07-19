package com.pal.dipesh.razorpay.merchant.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Getter
@Setter
@ToString
@Component
@EqualsAndHashCode
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MerchantContext {
    private UUID merchantId;
    private String keyId;
}
