package com.pal.dipesh.razorpay.common.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final String errorCode;
    private final long retryAfterSeconds;
    private final int remaining;

    public RateLimitExceededException(String errorCode, String message, long retryAfterSeconds, int remaining) {
        super(message);
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
        this.remaining = remaining;
    }
}
