package com.ridelink.fare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Receipt entity representing the official digital receipt issued after successful payment.
 */
@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipt_number", columnList = "receiptNumber", unique = true),
        @Index(name = "idx_receipt_payment_id", columnList = "paymentId", unique = true),
        @Index(name = "idx_receipt_ride_id", columnList = "rideId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String receiptNumber;

    @Column(nullable = false, unique = true)
    private Long paymentId;

    @Column(nullable = false)
    private Long rideId;

    @Column(nullable = false)
    private Long passengerAccountId;

    @Column(nullable = false)
    private Long driverAccountId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(length = 1000)
    private String breakdownSummary;
}
