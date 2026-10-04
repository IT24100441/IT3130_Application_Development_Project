package com.ridelink.ride.dto;

import com.ridelink.ride.entity.RideStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Full ride response DTO */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Complete ride information")
public class RideDTO {
    private Long id;
    private Long passengerAccountId;
    private Long driverAccountId;
    private Long driverProfileId;
    private Long fareId;
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String destinationAddress;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private Double estimatedDistanceKm;
    private BigDecimal estimatedFare;
    private BigDecimal finalFare;
    private RideStatus status;
    private String cancellationReason;
    private String assignedDriverName;
    private String assignedVehiclePlate;
    private LocalDateTime requestedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
