package com.ridelink.fare.service;

import com.ridelink.fare.dto.*;
import com.ridelink.fare.entity.*;
import com.ridelink.fare.exception.BusinessException;
import com.ridelink.fare.exception.PaymentProcessingException;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.repository.FareRepository;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing simulated payment processing, status tracking,
 * and automated digital receipt issuance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareRepository fareRepository;
    private final ReceiptRepository receiptRepository;

    /**
     * Processes simulated payment for a completed ride.
     * Generates a digital receipt on successful payment.
     */
    @Transactional
    public PaymentDTO processPayment(PaymentRequest request, Long authenticatedPassengerId) {
        log.info("Processing payment for rideId: {}, method: {}, passenger: {}",
                request.getRideId(), request.getPaymentMethod(), authenticatedPassengerId);

        Fare fare = fareRepository.findByRideId(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Cannot process payment. No fare calculated for ride ID: " + request.getRideId()));

        if (authenticatedPassengerId != null && !fare.getPassengerAccountId().equals(authenticatedPassengerId)) {
            throw new BusinessException("Unauthorized payment attempt: Authenticated user is not the passenger for ride " + request.getRideId());
        }

        if (paymentRepository.existsByRideIdAndStatus(request.getRideId(), PaymentStatus.COMPLETED)) {
            throw new BusinessException("Payment has already been completed for ride ID: " + request.getRideId());
        }

        String txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();

        // Simulated card decline logic for testing failure scenarios
        if (isSimulatedCardFailure(request)) {
            Payment failedPayment = Payment.builder()
                    .fareId(fare.getId())
                    .rideId(fare.getRideId())
                    .passengerAccountId(fare.getPassengerAccountId())
                    .amount(fare.getTotalFare())
                    .currency(fare.getCurrency())
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.FAILED)
                    .transactionReference(txnRef)
                    .failureReason("Simulated payment gateway failure: Insufficient funds or card decline.")
                    .build();

            paymentRepository.save(failedPayment);
            log.warn("Payment failed for rideId: {}, reason: {}", request.getRideId(), failedPayment.getFailureReason());
            throw new PaymentProcessingException("Payment transaction failed: Card declined or insufficient funds.");
        }

        // Successful Payment
        Payment payment = Payment.builder()
                .fareId(fare.getId())
                .rideId(fare.getRideId())
                .passengerAccountId(fare.getPassengerAccountId())
                .amount(fare.getTotalFare())
                .currency(fare.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.COMPLETED)
                .transactionReference(txnRef)
                .paidAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Automated Receipt Generation
        String receiptNumber = "RCPT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String summary = String.format("Ride #%d | Distance: %.1f km | Base: %s | Dist: %s | Time: %s | Total: %s %s",
                fare.getRideId(), fare.getDistanceKm(), fare.getBaseFare(),
                fare.getDistanceFare(), fare.getTimeFare(), fare.getTotalFare(), fare.getCurrency());

        Receipt receipt = Receipt.builder()
                .receiptNumber(receiptNumber)
                .paymentId(savedPayment.getId())
                .rideId(fare.getRideId())
                .passengerAccountId(fare.getPassengerAccountId())
                .driverAccountId(fare.getDriverAccountId())
                .totalAmount(fare.getTotalFare())
                .currency(fare.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .breakdownSummary(summary)
                .build();

        receiptRepository.save(receipt);
        log.info("Payment successful. TxnRef: {}, Receipt: {}", txnRef, receiptNumber);

        return toPaymentDTO(savedPayment, receiptNumber);
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        String receiptNum = receiptRepository.findByPaymentId(paymentId)
                .map(Receipt::getReceiptNumber).orElse(null);
        return toPaymentDTO(payment, receiptNum);
    }

    @Transactional(readOnly = true)
    public PaymentDTO getPaymentByRideId(Long rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record found for ride ID: " + rideId));
        String receiptNum = receiptRepository.findByRideId(rideId)
                .map(Receipt::getReceiptNumber).orElse(null);
        return toPaymentDTO(payment, receiptNum);
    }

    @Transactional(readOnly = true)
    public List<PaymentDTO> getPassengerPayments(Long passengerId) {
        return paymentRepository.findByPassengerAccountIdOrderByCreatedAtDesc(passengerId).stream()
                .map(p -> {
                    String receiptNum = receiptRepository.findByPaymentId(p.getId())
                            .map(Receipt::getReceiptNumber).orElse(null);
                    return toPaymentDTO(p, receiptNum);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReceiptDTO getReceiptByReceiptNumber(String receiptNumber) {
        Receipt receipt = receiptRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptNumber));
        return toReceiptDTO(receipt);
    }

    @Transactional(readOnly = true)
    public ReceiptDTO getReceiptByRideId(Long rideId) {
        Receipt receipt = receiptRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("No receipt issued for ride ID: " + rideId));
        return toReceiptDTO(receipt);
    }

    @Transactional(readOnly = true)
    public List<ReceiptDTO> getPassengerReceipts(Long passengerId) {
        return receiptRepository.findByPassengerAccountIdOrderByIssuedAtDesc(passengerId).stream()
                .map(this::toReceiptDTO)
                .collect(Collectors.toList());
    }

    private boolean isSimulatedCardFailure(PaymentRequest request) {
        if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD || request.getPaymentMethod() == PaymentMethod.DEBIT_CARD) {
            String card = request.getCardNumber();
            return card != null && (card.endsWith("0002") || card.endsWith("9999"));
        }
        return false;
    }

    private PaymentDTO toPaymentDTO(Payment p, String receiptNumber) {
        return PaymentDTO.builder()
                .paymentId(p.getId())
                .fareId(p.getFareId())
                .rideId(p.getRideId())
                .passengerAccountId(p.getPassengerAccountId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .transactionReference(p.getTransactionReference())
                .failureReason(p.getFailureReason())
                .paidAt(p.getPaidAt())
                .receiptNumber(receiptNumber)
                .build();
    }

    private ReceiptDTO toReceiptDTO(Receipt r) {
        return ReceiptDTO.builder()
                .receiptId(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .paymentId(r.getPaymentId())
                .rideId(r.getRideId())
                .passengerAccountId(r.getPassengerAccountId())
                .driverAccountId(r.getDriverAccountId())
                .totalAmount(r.getTotalAmount())
                .currency(r.getCurrency())
                .paymentMethod(r.getPaymentMethod())
                .issuedAt(r.getIssuedAt())
                .breakdownSummary(r.getBreakdownSummary())
                .build();
    }
}
