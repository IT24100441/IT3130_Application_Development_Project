package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estimated fare breakdown for planning a trip")
public class FareEstimateDTO {
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal timeFare;
    private BigDecimal surgeMultiplier;
    private BigDecimal estimatedTotalFare;
    private String currency;
}
