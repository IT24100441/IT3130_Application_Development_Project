package com.ridelink.account.dto;

import com.ridelink.account.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for new user registration.
 */
@Data
@Schema(description = "Payload to register a new user account")
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(description = "User's full name", example = "Sahan Perera")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Schema(description = "User's email address", example = "sahan@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Password (min 8 chars)", example = "SecurePass123!")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone must be a valid number (9–15 digits)")
    @Schema(description = "User's phone number", example = "+94771234567")
    private String phone;

    @NotNull(message = "Role is required")
    @Schema(description = "Role: PASSENGER or DRIVER", example = "PASSENGER")
    private Role role;
}
