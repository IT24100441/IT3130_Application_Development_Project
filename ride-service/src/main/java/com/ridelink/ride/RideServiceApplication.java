package com.ridelink.ride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Ride Management Service — Entry Point
 * Handles: ride lifecycle, driver assignment, state machine transitions.
 * Port: 8083
 * Uses OpenFeign to call driver-service and fare-service.
 */
@SpringBootApplication
@EnableFeignClients
public class RideServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideServiceApplication.class, args);
    }
}
