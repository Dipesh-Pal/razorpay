package com.pal.dipesh.razorpay.common.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String errorCode,
        String errorDescription,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {}

    public static ApiError of(String errorCode, String errorDescription) {
        return new ApiError(errorCode, errorDescription,null);
    }

    public static ApiError of(String errorCode, String errorDescription, List<FieldError> fieldErrors) {
        return new ApiError(errorCode, errorDescription, fieldErrors);
    }
}