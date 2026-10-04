package com.ridelink.account.entity;

/**
 * User roles in the RideLink system.
 * PASSENGER — can request rides
 * DRIVER    — can accept rides (must also have a DriverProfile in driver-service)
 * ADMIN     — system administrator
 */
public enum Role {
    PASSENGER,
    DRIVER,
    ADMIN
}
