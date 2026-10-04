package com.ridelink.fare.repository;

import com.ridelink.fare.entity.Payment;
import com.ridelink.fare.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRideId(Long rideId);
    Optional<Payment> findByRideIdAndStatus(Long rideId, PaymentStatus status);
    Optional<Payment> findByTransactionReference(String transactionReference);
    List<Payment> findByPassengerAccountIdOrderByCreatedAtDesc(Long passengerAccountId);
    boolean existsByRideIdAndStatus(Long rideId, PaymentStatus status);
}
