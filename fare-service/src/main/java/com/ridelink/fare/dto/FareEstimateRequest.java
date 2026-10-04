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
@Schema(description = "Request to estimate fare before requesting a ride")
public class FareEstimateRequest {

    @NotNull(message = "distanceKm is required")
    @Positive(message = "distanceKm must be positive")
    @Schema(description = "Estimated travel distance in kilometers", example = "12.0")
    private Double distanceKm;

    @Positive(message = "estimatedDurationMinutes must be positive if provided")
    @Schema(description = "Estimated travel duration in minutes (optional, estimated from speed if omitted)", example = "25")
    private Integer estimatedDurationMinutes;
}
