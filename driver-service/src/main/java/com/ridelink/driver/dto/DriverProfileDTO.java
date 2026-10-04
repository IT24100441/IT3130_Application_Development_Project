package com.ridelink.driver.dto;

import com.ridelink.driver.entity.AvailabilityStatus;
import com.ridelink.driver.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for driver profile with embedded vehicle info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Driver profile with vehicle details")
public class DriverProfileDTO {

    private Long id;
    private Long accountUserId;
    private String fullName;
    private String email;
    private String licenseNumber;
    private String serviceArea;
    private AvailabilityStatus availabilityStatus;
    private Double currentLatitude;
    private Double currentLongitude;
    private Boolean isVerified;

    // Embedded vehicle info
    private Long vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private VehicleType vehicleType;
    private Integer capacity;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
