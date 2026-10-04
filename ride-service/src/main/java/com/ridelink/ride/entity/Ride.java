package com.ridelink.ride.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core Ride entity — represents a single ride from request to completion.
 *
 * Cross-service IDs (no DB foreign keys):
 *   passengerAccountId  → User.id in account-service
 *   driverAccountId     → User.id in account-service (for the assigned driver)
 *   driverProfileId     → DriverProfile.id in driver-service
 *   fareId              → Fare.id in fare-service
 */
@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Cross-service stable IDs ───
    @Column(nullable = false)
    private Long passengerAccountId;

    private Long driverAccountId;

    private Long driverProfileId;

    private Long fareId;

    // ─── Ride details ───
    @Column(nullable = false, length = 255)
    private String pickupAddress;

    @Column(nullable = false)
    private Double pickupLatitude;

    @Column(nullable = false)
    private Double pickupLongitude;

    @Column(nullable = false, length = 255)
    private String destinationAddress;

    @Column(nullable = false)
    private Double destinationLatitude;

    @Column(nullable = false)
    private Double destinationLongitude;

    // ─── Computed values ───
    private Double estimatedDistanceKm;

    private BigDecimal estimatedFare;

    private BigDecimal finalFare;

    // ─── State machine ───
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    @Column(length = 500)
    private String cancellationReason;

    // ─── Driver info snapshot (for display without cross-service call) ───
    private String assignedDriverName;

    private String assignedVehiclePlate;

    // ─── Timestamps ───
    private LocalDateTime requestedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
