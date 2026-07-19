package com.pal.dipesh.razorpay.common.advice;

import com.pal.dipesh.razorpay.common.entity.ApiError;
import com.pal.dipesh.razorpay.common.entity.ApiResponse;
import com.pal.dipesh.razorpay.common.exception.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStateTransitionException(HttpServletRequest request, InvalidStateTransitionException ex) {
        ApiError apiError = ApiError.of("Payment Status Not Changed", ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, apiError, "Invalid_State_Transition", request);
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