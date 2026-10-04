package com.ridelink.fare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity representing simulated transaction records.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_ride_id", columnList = "rideId"),
        @Index(name = "idx_payment_txn_ref", columnList = "transactionReference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fareId;

    @Column(nullable = false)
    private Long rideId;

    @Column(nullable = false)
    private Long passengerAccountId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(nullable = false, unique = true, length = 100)
    private String transactionReference;

    @Column(length = 255)
    private String failureReason;

    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
