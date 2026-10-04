package com.ridelink.fare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fare entity capturing the comprehensive breakdown of ride pricing.
 * Database: Independent H2 in-memory faredb.
 */
@Entity
@Table(name = "fares", indexes = {
        @Index(name = "idx_fare_ride_id", columnList = "rideId", unique = true),
        @Index(name = "idx_fare_passenger_id", columnList = "passengerAccountId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long rideId;

    @Column(nullable = false)
    private Long passengerAccountId;

    @Column(nullable = false)
    private Long driverAccountId;

    @Column(nullable = false)
    private Double distanceKm;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceFare;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal timeFare;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal surgeMultiplier;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare;

    @Column(nullable = false, length = 10)
    private String currency;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime calculatedAt;
}
