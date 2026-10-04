package com.ridelink.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after successful login.
 * Contains the JWT bearer token and key user metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Successful authentication response with JWT")
public class AuthResponse {

    @Schema(description = "JWT Bearer token to use in Authorization header")
    private String accessToken;

    @Schema(description = "Token type, always 'Bearer'", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Token expiry in milliseconds", example = "86400000")
    private Long expiresIn;

    @Schema(description = "Authenticated user's details")
    private UserDTO user;
}
