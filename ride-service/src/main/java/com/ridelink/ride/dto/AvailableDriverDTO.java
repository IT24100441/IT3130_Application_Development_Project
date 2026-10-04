package com.ridelink.ride.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO matching driver-service's AvailableDriverDTO — used in Feign response deserialization */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AvailableDriverDTO {
    private Long driverProfileId;
    private Long accountUserId;
    private String fullName;
    private String plateNumber;
    private String vehicleDescription;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double distanceKm;
}
