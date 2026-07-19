package com.pal.dipesh.razorpay.common.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        ApiError error,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "UTC")
        LocalDateTime timestamp,
        String path
//        String traceId // TODO: Will handle later
) {

    public static <T> ApiResponse<T> success(T data,
                                             String message,
                                             HttpStatus status,
                                             String path) {
        return new ApiResponse<>(
                true,
                status.value(),
                message,
                data,
                null,
                LocalDateTime.now(),
                path
        );
    }

    public static <T> ApiResponse<T> failure(ApiError error,
                                             String message,
                                             HttpStatus status,
                                             String path) {
        return new ApiResponse<>(
                false,
                status.value(),
                message,
                null,
                error,
                LocalDateTime.now(),
                path
        );
    }
}