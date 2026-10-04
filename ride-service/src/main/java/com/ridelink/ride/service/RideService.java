package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.exception.*;
import com.ridelink.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Core Ride Management Service.
 *
 * State Machine transitions with validation:
 *   REQUESTED  → ASSIGNED     (system, after driver found)
 *   ASSIGNED   → ACCEPTED     (driver only)
 *   ACCEPTED   → IN_PROGRESS  (driver only)
 *   IN_PROGRESS→ COMPLETED    (driver only) → triggers fare calculation
 *   Any non-terminal → CANCELLED (passenger or driver)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RideService {

    private static final double SEARCH_RADIUS_KM = 15.0;
    // Base fare rules: LKR 150 base + LKR 80/km + LKR 3/min
    private static final BigDecimal BASE_FARE       = new BigDecimal("150.00");
    private static final BigDecimal RATE_PER_KM     = new BigDecimal("80.00");
    private static final BigDecimal RATE_PER_MINUTE = new BigDecimal("3.00");
    private static final double AVG_SPEED_KMPH = 40.0;

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;
    private final FareServiceClient fareServiceClient;

    // ─────────────────── Create Ride ───────────────────

    /**
     * Creates a new ride request and immediately attempts driver assignment.
     *
     * Workflow 1: ride-service → driver-service (OpenFeign) → assigns nearest driver.
     *
     * @param passengerAccountId authenticated passenger's userId
     * @param request            ride details
     * @return ride DTO (status ASSIGNED if driver found, REQUESTED if no driver available)
     */
    public RideDTO createRide(Long passengerAccountId, RideRequestDTO request) {
        log.info("Creating ride for passenger {}", passengerAccountId);

        // Prevent concurrent active rides
        List<Ride> activeRides = rideRepository.findByPassengerAccountIdAndStatus(
                passengerAccountId, RideStatus.REQUESTED);
        activeRides.addAll(rideRepository.findByPassengerAccountIdAndStatus(
                passengerAccountId, RideStatus.ASSIGNED));
        activeRides.addAll(rideRepository.findByPassengerAccountIdAndStatus(
                passengerAccountId, RideStatus.ACCEPTED));
        activeRides.addAll(rideRepository.findByPassengerAccountIdAndStatus(
                passengerAccountId, RideStatus.IN_PROGRESS));

        if (!activeRides.isEmpty()) {
            throw new BusinessException(
                    "You already have an active ride (ID: " + activeRides.get(0).getId() + "). " +
                    "Complete or cancel it before requesting a new one.");
        }

        double distanceKm = haversineDistanceKm(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDestinationLatitude(), request.getDestinationLongitude()
        );
        int estimatedMinutes = (int) Math.ceil((distanceKm / AVG_SPEED_KMPH) * 60);
        BigDecimal estimatedFare = calculateFareAmount(distanceKm, estimatedMinutes);

        Ride ride = Ride.builder()
                .passengerAccountId(passengerAccountId)
                .pickupAddress(request.getPickupAddress())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .destinationAddress(request.getDestinationAddress())
                .destinationLatitude(request.getDestinationLatitude())
                .destinationLongitude(request.getDestinationLongitude())
                .estimatedDistanceKm(Math.round(distanceKm * 100.0) / 100.0)
                .estimatedFare(estimatedFare)
                .status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        Ride saved = rideRepository.save(ride);

        // Attempt driver assignment via Feign call to driver-service
        try {
            List<AvailableDriverDTO> availableDrivers = driverServiceClient.getAvailableDrivers(
                    request.getPickupLatitude(), request.getPickupLongitude(), SEARCH_RADIUS_KM
            );

            if (!availableDrivers.isEmpty()) {
                AvailableDriverDTO nearest = availableDrivers.get(0);
                saved.setDriverAccountId(nearest.getAccountUserId());
                saved.setDriverProfileId(nearest.getDriverProfileId());
                saved.setAssignedDriverName(nearest.getFullName());
                saved.setAssignedVehiclePlate(nearest.getPlateNumber());
                saved.setStatus(RideStatus.ASSIGNED);
                saved.setAssignedAt(LocalDateTime.now());

                // Mark driver as ON_TRIP in driver-service
                driverServiceClient.updateDriverStatus(nearest.getDriverProfileId(), "ON_TRIP");

                log.info("Ride {} assigned to driver profile {}", saved.getId(), nearest.getDriverProfileId());
            } else {
                log.warn("No available drivers found for ride {}", saved.getId());
            }
        } catch (Exception ex) {
            log.warn("Driver assignment failed for ride {}: {}. Ride stays REQUESTED.",
                    saved.getId(), ex.getMessage());
            // Ride is saved but stays in REQUESTED — not a fatal error
        }

        return toDTO(rideRepository.save(saved));
    }

    // ─────────────────── Status Transitions ───────────────────

    /**
     * Advances ride to the next status in the state machine.
     * Each transition is validated against the allowed flow.
     *
     * @param rideId      ride to update
     * @param targetStatus target status
     * @param actorAccountId the account performing the action (for authorization)
     * @param isDriver    true if the caller is a DRIVER
     * @param request     optional cancellation reason
     */
    public RideDTO updateRideStatus(Long rideId, RideStatus targetStatus,
                                    Long actorAccountId, boolean isDriver,
                                    RideStatusUpdateRequest request) {

        Ride ride = findOrThrow(rideId);
        validateTransition(ride, targetStatus, actorAccountId, isDriver);

        RideStatus previousStatus = ride.getStatus();
        ride.setStatus(targetStatus);

        switch (targetStatus) {
            case ACCEPTED    -> ride.setAcceptedAt(LocalDateTime.now());
            case IN_PROGRESS -> ride.setStartedAt(LocalDateTime.now());
            case COMPLETED   -> {
                ride.setCompletedAt(LocalDateTime.now());
                triggerFareCalculation(ride);
            }
            case CANCELLED   -> {
                ride.setCancelledAt(LocalDateTime.now());
                ride.setCancellationReason(
                        request != null ? request.getCancellationReason() : "No reason provided");
                // Release driver if was assigned
                if (ride.getDriverProfileId() != null) {
                    try {
                        driverServiceClient.updateDriverStatus(ride.getDriverProfileId(), "ONLINE");
                    } catch (Exception ex) {
                        log.warn("Failed to release driver {} after cancellation: {}",
                                ride.getDriverProfileId(), ex.getMessage());
                    }
                }
            }
            default -> {}
        }

        log.info("Ride {} transitioned {} → {}", rideId, previousStatus, targetStatus);
        return toDTO(rideRepository.save(ride));
    }

    // ─────────────────── Retrieval ───────────────────

    @Transactional(readOnly = true)
    public RideDTO getRideById(Long rideId) {
        return toDTO(findOrThrow(rideId));
    }

    @Transactional(readOnly = true)
    public Page<RideDTO> getMyRides(Long accountId, boolean isDriver, Pageable pageable) {
        if (isDriver) {
            return rideRepository.findByDriverAccountIdOrderByCreatedAtDesc(accountId, pageable)
                    .map(this::toDTO);
        }
        return rideRepository.findByPassengerAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(this::toDTO);
    }

    // ─────────────────── State Machine Validation ───────────────────

    private static final Set<RideStatus> TERMINAL_STATES =
            Set.of(RideStatus.COMPLETED, RideStatus.CANCELLED);

    private void validateTransition(Ride ride, RideStatus target,
                                    Long actorAccountId, boolean isDriver) {
        RideStatus current = ride.getStatus();

        // Terminal states cannot transition
        if (TERMINAL_STATES.contains(current)) {
            throw new BusinessException(
                    "Ride " + ride.getId() + " is in terminal state " + current + ". No further transitions allowed.");
        }

        // Cancellation — allowed from any non-terminal state
        if (target == RideStatus.CANCELLED) {
            boolean isPassenger = ride.getPassengerAccountId().equals(actorAccountId);
            boolean isAssignedDriver = ride.getDriverAccountId() != null &&
                    ride.getDriverAccountId().equals(actorAccountId);
            if (!isPassenger && !isAssignedDriver) {
                throw new BusinessException("Only the passenger or assigned driver can cancel this ride.");
            }
            return;
        }

        // Normal progression
        boolean validTransition = switch (current) {
            case REQUESTED   -> target == RideStatus.ASSIGNED;
            case ASSIGNED    -> target == RideStatus.ACCEPTED;
            case ACCEPTED    -> target == RideStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == RideStatus.COMPLETED;
            default          -> false;
        };

        if (!validTransition) {
            throw new BusinessException(
                    "Invalid status transition: " + current + " → " + target);
        }

        // Role authorization: ACCEPTED / IN_PROGRESS / COMPLETED must be triggered by driver
        if (target == RideStatus.ACCEPTED || target == RideStatus.IN_PROGRESS || target == RideStatus.COMPLETED) {
            if (!isDriver || !ride.getDriverAccountId().equals(actorAccountId)) {
                throw new BusinessException(
                        "Only the assigned driver can perform transition to " + target);
            }
        }
    }

    // ─────────────────── Fare Trigger (Workflow 2) ───────────────────

    private void triggerFareCalculation(Ride ride) {
        try {
            int durationMins = (int) Math.ceil((ride.getEstimatedDistanceKm() / AVG_SPEED_KMPH) * 60);
            FareCalculationRequest fareRequest = FareCalculationRequest.builder()
                    .rideId(ride.getId())
                    .passengerAccountId(ride.getPassengerAccountId())
                    .driverAccountId(ride.getDriverAccountId())
                    .distanceKm(ride.getEstimatedDistanceKm())
                    .estimatedDurationMinutes(durationMins)
                    .build();

            FareDTO fareResponse = fareServiceClient.calculateFare(fareRequest);
            ride.setFareId(fareResponse.getFareId());
            ride.setFinalFare(fareResponse.getTotalFare());

            // Release driver back to ONLINE
            if (ride.getDriverProfileId() != null) {
                driverServiceClient.updateDriverStatus(ride.getDriverProfileId(), "ONLINE");
            }
            log.info("Fare {} calculated for ride {}: {}",
                    fareResponse.getFareId(), ride.getId(), fareResponse.getTotalFare());
        } catch (Exception ex) {
            log.error("Fare calculation failed for ride {}: {}", ride.getId(), ex.getMessage());
            // Non-fatal — ride is completed; fare service failure logged
        }
    }

    // ─────────────────── Helpers ───────────────────

    private Ride findOrThrow(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", rideId));
    }

    private BigDecimal calculateFareAmount(double distanceKm, int durationMinutes) {
        return BASE_FARE
                .add(RATE_PER_KM.multiply(BigDecimal.valueOf(distanceKm)))
                .add(RATE_PER_MINUTE.multiply(BigDecimal.valueOf(durationMinutes)));
    }

    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private RideDTO toDTO(Ride ride) {
        return RideDTO.builder()
                .id(ride.getId())
                .passengerAccountId(ride.getPassengerAccountId())
                .driverAccountId(ride.getDriverAccountId())
                .driverProfileId(ride.getDriverProfileId())
                .fareId(ride.getFareId())
                .pickupAddress(ride.getPickupAddress())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .destinationAddress(ride.getDestinationAddress())
                .destinationLatitude(ride.getDestinationLatitude())
                .destinationLongitude(ride.getDestinationLongitude())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .estimatedFare(ride.getEstimatedFare())
                .finalFare(ride.getFinalFare())
                .status(ride.getStatus())
                .cancellationReason(ride.getCancellationReason())
                .assignedDriverName(ride.getAssignedDriverName())
                .assignedVehiclePlate(ride.getAssignedVehiclePlate())
                .requestedAt(ride.getRequestedAt())
                .assignedAt(ride.getAssignedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .createdAt(ride.getCreatedAt())
                .updatedAt(ride.getUpdatedAt())
                .build();
    }
}
