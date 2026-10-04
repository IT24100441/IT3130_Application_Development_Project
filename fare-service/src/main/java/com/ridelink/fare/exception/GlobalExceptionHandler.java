package com.ridelink.fare.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                fieldErrors.put(((FieldError) e).getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("VALIDATION_FAILED")
                .message("Validation failed")
                .path(req.getRequestURI())
                .validationErrors(fieldErrors)
                .build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessing(PaymentProcessingException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(build(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PAYMENT_FAILED", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Access denied.", req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error in fare-service: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR",
                        "An unexpected server error occurred.", req.getRequestURI()));
    }

    private ErrorResponse build(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}
