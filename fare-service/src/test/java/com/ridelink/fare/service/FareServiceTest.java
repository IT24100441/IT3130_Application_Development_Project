package com.ridelink.fare.service;

import com.ridelink.fare.dto.*;
import com.ridelink.fare.entity.Fare;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.repository.FareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FareService Tests")
class FareServiceTest {

    @Mock
    private FareRepository fareRepository;

    @InjectMocks
    private FareService fareService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fareService, "baseFare", new BigDecimal("150.00"));
        ReflectionTestUtils.setField(fareService, "perKmRate", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(fareService, "perMinuteRate", new BigDecimal("3.00"));
        ReflectionTestUtils.setField(fareService, "surgeMultiplier", new BigDecimal("1.0"));
        ReflectionTestUtils.setField(fareService, "currency", "LKR");
    }

    @Nested
    @DisplayName("Fare Estimation Tests")
    class FareEstimationTests {

        @Test
        @DisplayName("Should estimate fare accurately with provided duration")
        void shouldEstimateFareWithDuration() {
            FareEstimateRequest request = FareEstimateRequest.builder()
                    .distanceKm(10.0)
                    .estimatedDurationMinutes(20)
                    .build();

            // Expected: Base (150) + 10km * 80 (800) + 20min * 3 (60) = 1010.00 LKR
            FareEstimateDTO estimate = fareService.estimateFare(request);

            assertThat(estimate).isNotNull();
            assertThat(estimate.getBaseFare()).isEqualByComparingTo("150.00");
            assertThat(estimate.getDistanceFare()).isEqualByComparingTo("800.00");
            assertThat(estimate.getTimeFare()).isEqualByComparingTo("60.00");
            assertThat(estimate.getEstimatedTotalFare()).isEqualByComparingTo("1010.00");
            assertThat(estimate.getCurrency()).isEqualTo("LKR");
        }

        @Test
        @DisplayName("Should estimate duration when omitted using 30 km/h baseline")
        void shouldEstimateDurationWhenOmitted() {
            FareEstimateRequest request = FareEstimateRequest.builder()
                    .distanceKm(15.0)
                    .build();

            // 15km at 30km/h = 30 minutes
            // Base (150) + 15 * 80 (1200) + 30 * 3 (90) = 1440.00 LKR
            FareEstimateDTO estimate = fareService.estimateFare(request);

            assertThat(estimate).isNotNull();
            assertThat(estimate.getEstimatedDurationMinutes()).isEqualTo(30);
            assertThat(estimate.getEstimatedTotalFare()).isEqualByComparingTo("1440.00");
        }
    }

    @Nested
    @DisplayName("Final Fare Calculation Tests")
    class FinalFareCalculationTests {

        @Test
        @DisplayName("Should calculate and persist final fare upon ride completion")
        void shouldCalculateFinalFare() {
            FareCalculationRequest request = FareCalculationRequest.builder()
                    .rideId(100L)
                    .passengerAccountId(1L)
                    .driverAccountId(2L)
                    .distanceKm(10.0)
                    .estimatedDurationMinutes(25)
                    .build();

            // 150 + (10 * 80 = 800) + (25 * 3 = 75) = 1025.00 LKR
            when(fareRepository.findByRideId(100L)).thenReturn(Optional.empty());
            when(fareRepository.save(any(Fare.class))).thenAnswer(invocation -> {
                Fare f = invocation.getArgument(0);
                f.setId(1L);
                f.setCalculatedAt(LocalDateTime.now());
                return f;
            });

            FareDTO result = fareService.calculateFinalFare(request);

            assertThat(result).isNotNull();
            assertThat(result.getRideId()).isEqualTo(100L);
            assertThat(result.getTotalFare()).isEqualByComparingTo("1025.00");
            assertThat(result.getCurrency()).isEqualTo("LKR");
            verify(fareRepository, times(1)).save(any(Fare.class));
        }

        @Test
        @DisplayName("Should return existing fare idempotently if already calculated")
        void shouldReturnExistingFareIfAlreadyCalculated() {
            FareCalculationRequest request = FareCalculationRequest.builder()
                    .rideId(100L)
                    .passengerAccountId(1L)
                    .driverAccountId(2L)
                    .distanceKm(10.0)
                    .estimatedDurationMinutes(25)
                    .build();

            Fare existing = Fare.builder()
                    .id(1L)
                    .rideId(100L)
                    .passengerAccountId(1L)
                    .driverAccountId(2L)
                    .distanceKm(10.0)
                    .durationMinutes(25)
                    .baseFare(new BigDecimal("150.00"))
                    .distanceFare(new BigDecimal("800.00"))
                    .timeFare(new BigDecimal("75.00"))
                    .surgeMultiplier(new BigDecimal("1.0"))
                    .totalFare(new BigDecimal("1025.00"))
                    .currency("LKR")
                    .calculatedAt(LocalDateTime.now())
                    .build();

            when(fareRepository.findByRideId(100L)).thenReturn(Optional.of(existing));

            FareDTO result = fareService.calculateFinalFare(request);

            assertThat(result).isNotNull();
            assertThat(result.getTotalFare()).isEqualByComparingTo("1025.00");
            verify(fareRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Fare Retrieval Tests")
    class FareRetrievalTests {

        @Test
        @DisplayName("Should retrieve fare by ride ID")
        void shouldRetrieveFareByRideId() {
            Fare existing = Fare.builder()
                    .id(1L)
                    .rideId(100L)
                    .passengerAccountId(1L)
                    .driverAccountId(2L)
                    .distanceKm(10.0)
                    .durationMinutes(25)
                    .baseFare(new BigDecimal("150.00"))
                    .distanceFare(new BigDecimal("800.00"))
                    .timeFare(new BigDecimal("75.00"))
                    .surgeMultiplier(new BigDecimal("1.0"))
                    .totalFare(new BigDecimal("1025.00"))
                    .currency("LKR")
                    .build();

            when(fareRepository.findByRideId(100L)).thenReturn(Optional.of(existing));

            FareDTO result = fareService.getFareByRideId(100L);

            assertThat(result.getRideId()).isEqualTo(100L);
            assertThat(result.getTotalFare()).isEqualByComparingTo("1025.00");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when fare not found")
        void shouldThrowWhenFareNotFound() {
            when(fareRepository.findByRideId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fareService.getFareByRideId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }
}
