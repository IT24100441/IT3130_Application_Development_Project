package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating a new driver operational profile.
 * The accountUserId is the JWT userId claim — not user-supplied.
 */
@Data
@Schema(description = "Driver profile registration payload")
public class DriverRegistrationRequest {

    @NotBlank(message = "License number is required")
    @Size(min = 5, max = 50)
    @Schema(example = "LK-2024-00123")
    private String licenseNumber;

    @NotBlank(message = "Service area is required")
    @Size(max = 100)
    @Schema(example = "Colombo")
    private String serviceArea;

    @Valid
    @NotNull(message = "Vehicle details are required")
    private VehicleRegistrationRequest vehicle;
}
