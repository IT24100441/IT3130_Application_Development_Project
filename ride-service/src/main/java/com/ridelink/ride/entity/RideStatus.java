package com.ridelink.ride.entity;

/**
 * Ride State Machine:
 *
 *  REQUESTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED
 *                                                ↘ CANCELLED
 *
 *  CANCELLED is reachable from any non-terminal state.
 *  Terminal states: COMPLETED, CANCELLED (no further transitions allowed).
 */
public enum RideStatus {
    REQUESTED,    // Passenger requested; no driver yet
    ASSIGNED,     // Driver assigned by system (driver hasn't confirmed yet)
    ACCEPTED,     // Driver confirmed the ride
    IN_PROGRESS,  // Ride started; driver picked up passenger
    COMPLETED,    // Ride finished — triggers fare calculation
    CANCELLED     // Ride cancelled by passenger or driver
}
