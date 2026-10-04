package com.ridelink.ride.client;

import com.ridelink.ride.dto.AvailableDriverDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OpenFeign client for driver-service.
 *
 * Communication: Synchronous REST via OpenFeign.
 * Rationale: Driver availability lookup is a blocking, real-time requirement
 * (we cannot assign a ride without knowing if a driver is available right now).
 * Synchronous is appropriate here; async would complicate the ride creation flow.
 *
 * URL is overridden per-environment via 'service.driver.url' property.
 */
@FeignClient(
        name = "driver-service",
        url = "${service.driver.url}",
        configuration = com.ridelink.ride.config.FeignConfig.class
)
public interface DriverServiceClient {

    /**
     * Fetches ONLINE+verified drivers within radius km of the pickup point.
     */
    @GetMapping("/api/drivers/available")
    List<AvailableDriverDTO> getAvailableDrivers(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam("radius") double radius
    );

    /**
     * Updates a driver's availability status (e.g., ON_TRIP, ONLINE).
     */
    @PatchMapping("/api/drivers/internal/{driverProfileId}/status")
    void updateDriverStatus(
            @PathVariable("driverProfileId") Long driverProfileId,
            @RequestParam("status") String status
    );
}
