package com.ridelink.driver.entity;

public enum AvailabilityStatus {
    ONLINE,
    OFFLINE,
    ON_TRIP  // Set by ride-service when driver is assigned an active ride
}
