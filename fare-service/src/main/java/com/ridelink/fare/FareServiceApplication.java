package com.ridelink.fare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fare &amp; Payment Service — Entry Point
 * Handles: fare estimation, calculation, payments, and receipt generation.
 * Port: 8084
 */
@SpringBootApplication
public class FareServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FareServiceApplication.class, args);
    }
}
