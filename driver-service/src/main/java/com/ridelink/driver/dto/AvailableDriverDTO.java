package com.ridelink.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO returned to ride-service when querying available drivers.
 * Contains only the fields needed for driver assignment decisions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Available driver summary used by ride-service for assignment")
public class AvailableDriverDTO {

    @Schema(description = "Driver profile ID", example = "1")
    private Long driverProfileId;

    @Schema(description = "Stable cross-service user ID from account-service", example = "3")
    private Long accountUserId;

    @Schema(description = "Driver's full name", example = "Kamal Silva")
    private String fullName;

    @Schema(description = "Vehicle plate number", example = "CAB-1234")
    private String plateNumber;

    @Schema(description = "Vehicle make and model", example = "Toyota Prius")
    private String vehicleDescription;

    @Schema(description = "Current driver latitude", example = "6.9271")
    private Double currentLatitude;

    @Schema(description = "Current driver longitude", example = "79.8612")
    private Double currentLongitude;

    @Schema(description = "Distance from pickup point in km", example = "2.3")
    private Double distanceKm;
}
