package com.ridelink.driver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Vehicle associated with a DriverProfile.
 * One driver has exactly one vehicle.
 */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id", nullable = false, unique = true)
    private DriverProfile driverProfile;

    @Column(nullable = false, length = 20)
    private String plateNumber;

    @Column(nullable = false, length = 50)
    private String make;  // e.g., Toyota

    @Column(nullable = false, length = 50)
    private String model; // e.g., Prius

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 4;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
