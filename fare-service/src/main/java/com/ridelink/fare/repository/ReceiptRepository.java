package com.ridelink.fare.repository;

import com.ridelink.fare.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    Optional<Receipt> findByPaymentId(Long paymentId);
    Optional<Receipt> findByRideId(Long rideId);
    List<Receipt> findByPassengerAccountIdOrderByIssuedAtDesc(Long passengerAccountId);
    List<Receipt> findByDriverAccountIdOrderByIssuedAtDesc(Long driverAccountId);
}
