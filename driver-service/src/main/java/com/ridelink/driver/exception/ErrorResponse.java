package com.ridelink.driver.exception;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}
