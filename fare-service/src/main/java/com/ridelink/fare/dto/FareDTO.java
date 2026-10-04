package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Calculated fare details response")
public class FareDTO {
    private Long fareId;
    private Long rideId;
    private Long passengerAccountId;
    private Long driverAccountId;
    private Double distanceKm;
    private Integer durationMinutes;
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal timeFare;
    private BigDecimal surgeMultiplier;
    private BigDecimal totalFare;
    private String currency;
    private LocalDateTime calculatedAt;
}
