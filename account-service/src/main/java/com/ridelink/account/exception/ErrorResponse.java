package com.ridelink.account.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardised error response body returned for all API errors.
 * Every error from every service uses this shape for consistency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** HTTP timestamp */
    private LocalDateTime timestamp;

    /** HTTP status code (e.g., 400, 401, 404) */
    private int status;

    /** Short error name (e.g., "BAD_REQUEST", "NOT_FOUND") */
    private String error;

    /** Human-readable error message */
    private String message;

    /** Request path that triggered the error */
    private String path;

    /** Field-level validation errors (null when not applicable) */
    private Map<String, String> validationErrors;
}
