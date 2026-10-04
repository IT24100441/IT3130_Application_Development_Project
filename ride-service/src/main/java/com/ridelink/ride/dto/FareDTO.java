package com.ridelink.ride.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Fare response from fare-service — used to update the ride record */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FareDTO {
    private Long fareId;
    private Long rideId;
    private BigDecimal totalFare;
    private String currency;
}
