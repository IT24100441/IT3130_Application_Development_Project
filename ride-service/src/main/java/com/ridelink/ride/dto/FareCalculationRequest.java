package com.ridelink.ride.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Request DTO sent to fare-service for fare calculation */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FareCalculationRequest {
    @Schema(description = "Ride ID in ride-service DB")
    private Long rideId;

    @Schema(description = "Passenger's account user ID")
    private Long passengerAccountId;

    @Schema(description = "Driver's account user ID")
    private Long driverAccountId;

    @Schema(description = "Calculated distance in km", example = "25.4")
    private Double distanceKm;

    @Schema(description = "Estimated ride duration in minutes", example = "40")
    private Integer estimatedDurationMinutes;
}
