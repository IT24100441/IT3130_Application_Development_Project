package com.ridelink.ride.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating a new ride.
 */
@Data
@Schema(description = "Payload to request a new ride")
public class RideRequestDTO {

    @NotBlank(message = "Pickup address is required")
    @Size(max = 255)
    @Schema(example = "123 Galle Road, Colombo 03")
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    @Schema(example = "6.9271")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    @Schema(example = "79.8612")
    private Double pickupLongitude;

    @NotBlank(message = "Destination address is required")
    @Size(max = 255)
    @Schema(example = "Bandaranaike Airport, Katunayake")
    private String destinationAddress;

    @NotNull(message = "Destination latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    @Schema(example = "7.1800")
    private Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    @Schema(example = "79.8845")
    private Double destinationLongitude;
}
