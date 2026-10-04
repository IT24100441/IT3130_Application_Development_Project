package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to trigger final fare calculation upon ride completion")
public class FareCalculationRequest {

    @NotNull(message = "rideId is required")
    @Schema(description = "ID of the ride", example = "101")
    private Long rideId;

    @NotNull(message = "passengerAccountId is required")
    @Schema(description = "Passenger account user ID", example = "1")
    private Long passengerAccountId;

    @NotNull(message = "driverAccountId is required")
    @Schema(description = "Driver account user ID", example = "2")
    private Long driverAccountId;

    @NotNull(message = "distanceKm is required")
    @Positive(message = "distanceKm must be positive")
    @Schema(description = "Actual ride distance in km", example = "14.5")
    private Double distanceKm;

    @NotNull(message = "estimatedDurationMinutes is required")
    @Positive(message = "estimatedDurationMinutes must be positive")
    @Schema(description = "Actual or estimated ride duration in minutes", example = "28")
    private Integer estimatedDurationMinutes;
}
