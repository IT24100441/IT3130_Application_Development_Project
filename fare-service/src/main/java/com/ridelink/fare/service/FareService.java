package com.ridelink.fare.service;

import com.ridelink.fare.dto.*;
import com.ridelink.fare.entity.Fare;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.repository.FareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Service handling fare estimation and final fare calculations.
 *
 * Documented Fare Calculation Formula:
 * -----------------------------------
 * Total Fare = (Base Fare + (Distance in km * Per-Km Rate) + (Duration in min * Per-Minute Rate)) * Surge Multiplier
 *
 * Configurable properties:
 * - fare.base: Base fare (default 150.00 LKR)
 * - fare.per-km: Cost per km (default 80.00 LKR)
 * - fare.per-minute: Cost per minute (default 3.00 LKR)
 * - fare.surge-multiplier: Demand surge multiplier (default 1.0x)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FareService {

    private final FareRepository fareRepository;

    @Value("${fare.base:150.00}")
    private BigDecimal baseFare;

    @Value("${fare.per-km:80.00}")
    private BigDecimal perKmRate;

    @Value("${fare.per-minute:3.00}")
    private BigDecimal perMinuteRate;

    @Value("${fare.surge-multiplier:1.0}")
    private BigDecimal surgeMultiplier;

    @Value("${fare.currency:LKR}")
    private String currency;

    /**
     * Estimates trip fare before ride booking.
     */
    @Transactional(readOnly = true)
    public FareEstimateDTO estimateFare(FareEstimateRequest request) {
        log.info("Estimating fare for distance: {} km", request.getDistanceKm());

        int duration = request.getEstimatedDurationMinutes() != null
                ? request.getEstimatedDurationMinutes()
                : Math.max(5, (int) Math.round((request.getDistanceKm() / 30.0) * 60)); // Assume 30 km/h average speed

        BigDecimal distFare = perKmRate.multiply(BigDecimal.valueOf(request.getDistanceKm()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeFare = perMinuteRate.multiply(BigDecimal.valueOf(duration))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = baseFare.add(distFare).add(timeFare);
        BigDecimal total = subtotal.multiply(surgeMultiplier).setScale(2, RoundingMode.HALF_UP);

        return FareEstimateDTO.builder()
                .distanceKm(request.getDistanceKm())
                .estimatedDurationMinutes(duration)
                .baseFare(baseFare)
                .distanceFare(distFare)
                .timeFare(timeFare)
                .surgeMultiplier(surgeMultiplier)
                .estimatedTotalFare(total)
                .currency(currency)
                .build();
    }

    /**
     * Calculates and persists the final fare upon ride completion.
     * Idempotent: If already calculated for this rideId, returns the existing record.
     */
    @Transactional
    public FareDTO calculateFinalFare(FareCalculationRequest request) {
        log.info("Calculating final fare for rideId: {}, distance: {} km, duration: {} min",
                request.getRideId(), request.getDistanceKm(), request.getEstimatedDurationMinutes());

        Optional<Fare> existing = fareRepository.findByRideId(request.getRideId());
        if (existing.isPresent()) {
            log.info("Fare already exists for rideId: {}, returning existing record", request.getRideId());
            return toDTO(existing.get());
        }

        BigDecimal distFare = perKmRate.multiply(BigDecimal.valueOf(request.getDistanceKm()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal timeFare = perMinuteRate.multiply(BigDecimal.valueOf(request.getEstimatedDurationMinutes()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = baseFare.add(distFare).add(timeFare);
        BigDecimal total = subtotal.multiply(surgeMultiplier).setScale(2, RoundingMode.HALF_UP);

        Fare fare = Fare.builder()
                .rideId(request.getRideId())
                .passengerAccountId(request.getPassengerAccountId())
                .driverAccountId(request.getDriverAccountId())
                .distanceKm(request.getDistanceKm())
                .durationMinutes(request.getEstimatedDurationMinutes())
                .baseFare(baseFare)
                .distanceFare(distFare)
                .timeFare(timeFare)
                .surgeMultiplier(surgeMultiplier)
                .totalFare(total)
                .currency(currency)
                .build();

        Fare saved = fareRepository.save(fare);
        log.info("Final fare calculated successfully. Fare ID: {}, Total: {} {}", saved.getId(), saved.getTotalFare(), saved.getCurrency());
        return toDTO(saved);
    }

    /**
     * Retrieves calculated fare details by ride ID.
     */
    @Transactional(readOnly = true)
    public FareDTO getFareByRideId(Long rideId) {
        Fare fare = fareRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare record not found for ride ID: " + rideId));
        return toDTO(fare);
    }

    /**
     * Retrieves calculated fare details by fare ID.
     */
    @Transactional(readOnly = true)
    public FareDTO getFareById(Long fareId) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare record not found with ID: " + fareId));
        return toDTO(fare);
    }

    private FareDTO toDTO(Fare fare) {
        return FareDTO.builder()
                .fareId(fare.getId())
                .rideId(fare.getRideId())
                .passengerAccountId(fare.getPassengerAccountId())
                .driverAccountId(fare.getDriverAccountId())
                .distanceKm(fare.getDistanceKm())
                .durationMinutes(fare.getDurationMinutes())
                .baseFare(fare.getBaseFare())
                .distanceFare(fare.getDistanceFare())
                .timeFare(fare.getTimeFare())
                .surgeMultiplier(fare.getSurgeMultiplier())
                .totalFare(fare.getTotalFare())
                .currency(fare.getCurrency())
                .calculatedAt(fare.getCalculatedAt())
                .build();
    }
}
