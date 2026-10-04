package com.ridelink.account.dto;

import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbound DTO for user profile data.
 * Passwords are never exposed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile information")
public class UserDTO {

    @Schema(description = "Unique user identifier", example = "1")
    private Long id;

    @Schema(description = "User's full name", example = "Sahan Perera")
    private String fullName;

    @Schema(description = "Email address", example = "sahan@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+94771234567")
    private String phone;

    @Schema(description = "User role", example = "PASSENGER")
    private Role role;

    @Schema(description = "Account status", example = "ACTIVE")
    private AccountStatus status;

    @Schema(description = "Profile picture URL")
    private String profilePictureUrl;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
