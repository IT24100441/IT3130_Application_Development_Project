package com.ridelink.fare.service;

import com.ridelink.fare.dto.PaymentDTO;
import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.ReceiptDTO;
import com.ridelink.fare.entity.*;
import com.ridelink.fare.exception.BusinessException;
import com.ridelink.fare.exception.PaymentProcessingException;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.repository.FareRepository;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FareRepository fareRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Fare testFare;

    @BeforeEach
    void setUp() {
        testFare = Fare.builder()
                .id(1L)
                .rideId(50L)
                .passengerAccountId(10L)
                .driverAccountId(20L)
                .distanceKm(12.5)
                .durationMinutes(20)
                .baseFare(new BigDecimal("150.00"))
                .distanceFare(new BigDecimal("1000.00"))
                .timeFare(new BigDecimal("60.00"))
                .surgeMultiplier(new BigDecimal("1.0"))
                .totalFare(new BigDecimal("1210.00"))
                .currency("LKR")
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Process Payment Tests")
    class ProcessPaymentTests {

        @Test
        @DisplayName("Should successfully process payment and issue receipt")
        void shouldProcessPaymentSuccessfully() {
            PaymentRequest request = PaymentRequest.builder()
                    .rideId(50L)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .cardNumber("4111222233334444")
                    .cvv("123")
                    .build();

            when(fareRepository.findByRideId(50L)).thenReturn(Optional.of(testFare));
            when(paymentRepository.existsByRideIdAndStatus(50L, PaymentStatus.COMPLETED)).thenReturn(false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(100L);
                return p;
            });
            when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> {
                Receipt r = inv.getArgument(0);
                r.setId(200L);
                return r;
            });

            PaymentDTO response = paymentService.processPayment(request, 10L);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(response.getAmount()).isEqualByComparingTo("1210.00");
            assertThat(response.getReceiptNumber()).startsWith("RCPT-");
            verify(paymentRepository, times(1)).save(any(Payment.class));
            verify(receiptRepository, times(1)).save(any(Receipt.class));
        }

        @Test
        @DisplayName("Should simulate card decline for test cards ending in 0002")
        void shouldFailPaymentForDeclineCard() {
            PaymentRequest request = PaymentRequest.builder()
                    .rideId(50L)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .cardNumber("4000000000000002")
                    .cvv("999")
                    .build();

            when(fareRepository.findByRideId(50L)).thenReturn(Optional.of(testFare));
            when(paymentRepository.existsByRideIdAndStatus(50L, PaymentStatus.COMPLETED)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.processPayment(request, 10L))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessageContaining("Card declined");

            verify(paymentRepository, times(1)).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
            verify(receiptRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject payment if already completed")
        void shouldRejectDuplicatePayment() {
            PaymentRequest request = PaymentRequest.builder()
                    .rideId(50L)
                    .paymentMethod(PaymentMethod.CASH)
                    .build();

            when(fareRepository.findByRideId(50L)).thenReturn(Optional.of(testFare));
            when(paymentRepository.existsByRideIdAndStatus(50L, PaymentStatus.COMPLETED)).thenReturn(true);

            assertThatThrownBy(() -> paymentService.processPayment(request, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already been completed");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when no fare exists for ride")
        void shouldThrowWhenNoFareFound() {
            PaymentRequest request = PaymentRequest.builder()
                    .rideId(999L)
                    .paymentMethod(PaymentMethod.CASH)
                    .build();

            when(fareRepository.findByRideId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPayment(request, 10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Should reject payment if authenticated user is not the ride's passenger")
        void shouldRejectUnauthorizedPassenger() {
            PaymentRequest request = PaymentRequest.builder()
                    .rideId(50L)
                    .paymentMethod(PaymentMethod.CASH)
                    .build();

            when(fareRepository.findByRideId(50L)).thenReturn(Optional.of(testFare));

            assertThatThrownBy(() -> paymentService.processPayment(request, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Unauthorized payment attempt");
        }
    }

    @Nested
    @DisplayName("Receipt Retrieval Tests")
    class ReceiptRetrievalTests {

        @Test
        @DisplayName("Should retrieve receipt by receipt number")
        void shouldRetrieveReceiptByNumber() {
            Receipt receipt = Receipt.builder()
                    .id(1L)
                    .receiptNumber("RCPT-12345")
                    .paymentId(10L)
                    .rideId(50L)
                    .passengerAccountId(10L)
                    .driverAccountId(20L)
                    .totalAmount(new BigDecimal("1210.00"))
                    .currency("LKR")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .issuedAt(LocalDateTime.now())
                    .breakdownSummary("Ride #50 | Total: 1210.00 LKR")
                    .build();

            when(receiptRepository.findByReceiptNumber("RCPT-12345")).thenReturn(Optional.of(receipt));

            ReceiptDTO dto = paymentService.getReceiptByReceiptNumber("RCPT-12345");

            assertThat(dto.getReceiptNumber()).isEqualTo("RCPT-12345");
            assertThat(dto.getTotalAmount()).isEqualByComparingTo("1210.00");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if receipt not found")
        void shouldThrowWhenReceiptNotFound() {
            when(receiptRepository.findByReceiptNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getReceiptByReceiptNumber("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("INVALID");
        }
    }
}
