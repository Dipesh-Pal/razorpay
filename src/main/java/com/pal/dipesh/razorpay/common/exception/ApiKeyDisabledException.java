package com.pal.dipesh.razorpay.common.exception;

import lombok.Getter;

@Getter
public class ApiKeyDisabledException extends RuntimeException {
    private final String errorCode;

    public ApiKeyDisabledException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}