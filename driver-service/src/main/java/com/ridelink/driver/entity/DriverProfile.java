package com.ridelink.driver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores the driver's operational profile.
 * accountUserId — the stable cross-service identifier (User.id from account-service).
 * No foreign key to account-service DB — microservice boundary respected.
 */
@Entity
@Table(name = "driver_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_driver_account_user_id", columnNames = "accountUserId"),
        @UniqueConstraint(name = "uk_driver_license_number", columnNames = "licenseNumber")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable cross-service ID from account-service */
    @Column(nullable = false, unique = true)
    private Long accountUserId;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 50)
    private String licenseNumber;

    @Column(nullable = false, length = 50)
    private String serviceArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;

    /** Simulated GPS latitude */
    @Builder.Default
    private Double currentLatitude = 0.0;

    /** Simulated GPS longitude */
    @Builder.Default
    private Double currentLongitude = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @OneToOne(mappedBy = "driverProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Vehicle vehicle;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
