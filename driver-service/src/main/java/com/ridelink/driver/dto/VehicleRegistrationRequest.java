package com.ridelink.driver.dto;

import com.ridelink.driver.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for registering a vehicle during driver profile creation.
 */
@Data
@Schema(description = "Vehicle registration details")
public class VehicleRegistrationRequest {

    @NotBlank(message = "Plate number is required")
    @Size(max = 20)
    @Schema(example = "CAB-1234")
    private String plateNumber;

    @NotBlank(message = "Make is required")
    @Size(max = 50)
    @Schema(example = "Toyota")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50)
    @Schema(example = "Prius")
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Vehicle year must be 2000 or later")
    @Max(value = 2030, message = "Vehicle year must be realistic")
    @Schema(example = "2022")
    private Integer year;

    @NotBlank(message = "Color is required")
    @Size(max = 30)
    @Schema(example = "Silver")
    private String color;

    @NotNull(message = "Vehicle type is required")
    @Schema(example = "SEDAN")
    private VehicleType vehicleType;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 12, message = "Capacity cannot exceed 12")
    @Schema(example = "4")
    private Integer capacity = 4;
}
