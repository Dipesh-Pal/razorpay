package com.pal.dipesh.razorpay.common.advice;

import com.pal.dipesh.razorpay.common.pojo.ApiError;
import com.pal.dipesh.razorpay.common.pojo.ApiResponse;
import com.pal.dipesh.razorpay.common.exception.*;
import com.pal.dipesh.razorpay.common.util.CookieUtil;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final CookieUtil cookieUtil;

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(HttpServletRequest request, DuplicateResourceException ex) {
        ApiError apiError = ApiError.of(ex.getErrorCode(), ex.getMessage());

        return buildErrorResponse(HttpStatus.CONFLICT, apiError, "Resource Creation Failed", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(HttpServletRequest request, ResourceNotFoundException ex) {
        String errorCode = ex.getResourceName().toUpperCase() + "_NOT_FOUND";

        ApiError apiError = ApiError.of(errorCode, ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_FOUND, apiError, "Resource Not Found", request);
    }

    @ExceptionHandler(ApiKeyDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyDisabledException(HttpServletRequest request, ApiKeyDisabledException ex) {
        ApiError apiError = ApiError.of(ex.getErrorCode(), ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, apiError, "API Key Disabled", request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleViolationException(HttpServletRequest request, BusinessRuleViolationException ex) {
        ApiError apiError = ApiError.of(ex.getErrorCode(), ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, apiError, "Business Rule Violation", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(HttpServletRequest request, MethodArgumentNotValidException ex) {
        String errorCode = "INPUT_VALIDATION_ERROR";

        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        ApiError apiError = ApiError.of(errorCode, "Request Validation Failed", fieldErrors);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, apiError, "Input Validation Failed", request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshTokenException(HttpServletRequest request, InvalidRefreshTokenException ex) {
        ApiError apiError = ApiError.of(ex.getErrorCode(), ex.getMessage());

        // Clear the refresh cookie so a compromised / stale value stops being replayed.
        ResponseCookie cleared = cookieUtil.clearRefreshCookie();

        return buildErrorResponse(HttpStatus.UNAUTHORIZED, apiError, "Refresh token rejected", request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStateTransitionException(HttpServletRequest request, InvalidStateTransitionException ex) {
        ApiError apiError = ApiError.of("PAYMENT_STATUS_TRANSITION_ERROR", ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, apiError, "Invalid State Transition", request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(HttpServletRequest request, RateLimitExceededException ex) {
        ApiError apiError = ApiError.of(ex.getErrorCode(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .header("X-RateLimit-Remaining", String.valueOf(ex.getRemaining()))
                .header("X-RateLimit-Reset", String.valueOf(Instant.now().plusSeconds(ex.getRetryAfterSeconds()).getEpochSecond()))
                .body(ApiResponse.failure(
                        apiError,
                        "Rate Limit Exceeded",
                        HttpStatus.TOO_MANY_REQUESTS,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IdemPotencyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdemPotencyConflictException(HttpServletRequest request, IdemPotencyConflictException ex) {
        ApiError apiError = ApiError.of("IDEMPOTENT_CONFLICT", ex.getMessage());

        return buildErrorResponse(HttpStatus.CONFLICT, apiError, "Idempotency Conflict", request);
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, ApiError apiError, String message, HttpServletRequest request) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(
                        apiError,
                        message,
                        status,
                        request.getRequestURI()
                ));
    }
}