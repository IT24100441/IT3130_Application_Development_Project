package com.ridelink.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Driver &amp; Vehicle Service — Entry Point
 * Handles: driver profiles, vehicles, availability toggle, location tracking.
 * Port: 8082
 */
@SpringBootApplication
public class DriverServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverServiceApplication.class, args);
    }
}
