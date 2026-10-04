package com.ridelink.ride.client;

import com.ridelink.ride.dto.FareCalculationRequest;
import com.ridelink.ride.dto.FareDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * OpenFeign client for fare-service.
 *
 * Communication: Synchronous REST via OpenFeign.
 * Rationale: Fare calculation is triggered synchronously upon ride completion
 * so the passenger immediately sees the final fare in the ride response.
 * Decoupled enough for future async upgrade with event streaming.
 */
@FeignClient(
        name = "fare-service",
        url = "${service.fare.url}",
        configuration = com.ridelink.ride.config.FeignConfig.class
)
public interface FareServiceClient {

    /**
     * Triggers fare calculation for a completed ride.
     * Called when ride status transitions to COMPLETED.
     */
    @PostMapping("/api/fares/calculate")
    FareDTO calculateFare(@RequestBody FareCalculationRequest request);
}
