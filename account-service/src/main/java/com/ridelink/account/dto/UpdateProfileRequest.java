package com.ridelink.account.dto;

import com.ridelink.account.entity.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating user profile fields.
 * All fields are optional — only non-null values will be applied.
 */
@Data
@Schema(description = "Payload to update user profile (all fields optional)")
public class UpdateProfileRequest {

    @Size(min = 2, max = 100)
    @Schema(description = "New full name", example = "Sahan K. Perera")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone must be valid")
    @Schema(description = "New phone number", example = "+94771239999")
    private String phone;

    @Schema(description = "Profile picture URL")
    private String profilePictureUrl;
}
