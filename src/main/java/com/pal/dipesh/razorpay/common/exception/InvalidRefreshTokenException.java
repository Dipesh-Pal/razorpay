package com.pal.dipesh.razorpay.common.exception;

import lombok.Getter;

@Getter
public class InvalidRefreshTokenException extends RuntimeException {

    private final String errorCode;

    public InvalidRefreshTokenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidRefreshTokenException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
