package com.ridelink.driver.service;

import com.ridelink.driver.dto.*;
import com.ridelink.driver.entity.*;
import com.ridelink.driver.exception.BusinessException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic for driver profiles, vehicles, availability, and location.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;

    // ─────────────────── Profile Registration ───────────────────

    /**
     * Creates a driver profile for an account-service user.
     *
     * @param accountUserId  userId from JWT claims
     * @param fullName       from JWT claims
     * @param email          from JWT claims
     * @param request        registration details
     */
    public DriverProfileDTO registerDriverProfile(Long accountUserId, String fullName,
                                                   String email, DriverRegistrationRequest request) {

        if (driverProfileRepository.existsByAccountUserId(accountUserId)) {
            throw new BusinessException("A driver profile already exists for this account.");
        }

        if (driverProfileRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BusinessException("License number already registered: " + request.getLicenseNumber());
        }

        VehicleRegistrationRequest vr = request.getVehicle();
        if (vehicleRepository.existsByPlateNumber(vr.getPlateNumber())) {
            throw new BusinessException("Vehicle plate already registered: " + vr.getPlateNumber());
        }

        DriverProfile profile = DriverProfile.builder()
                .accountUserId(accountUserId)
                .fullName(fullName)
                .email(email)
                .licenseNumber(request.getLicenseNumber())
                .serviceArea(request.getServiceArea())
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .currentLatitude(0.0)
                .currentLongitude(0.0)
                .isVerified(false)
                .build();

        DriverProfile savedProfile = driverProfileRepository.save(profile);

        Vehicle vehicle = Vehicle.builder()
                .driverProfile(savedProfile)
                .plateNumber(vr.getPlateNumber())
                .make(vr.getMake())
                .model(vr.getModel())
                .year(vr.getYear())
                .color(vr.getColor())
                .vehicleType(vr.getVehicleType())
                .capacity(vr.getCapacity())
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        savedProfile.setVehicle(savedVehicle);

        log.info("Driver profile created for accountUserId={}", accountUserId);
        return toDTO(savedProfile, savedVehicle);
    }

    // ─────────────────── Profile Retrieval ───────────────────

    @Transactional(readOnly = true)
    public DriverProfileDTO getDriverProfileByAccountId(Long accountUserId) {
        DriverProfile profile = driverProfileRepository.findByAccountUserId(accountUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver profile not found for accountUserId: " + accountUserId));
        return toDTO(profile, profile.getVehicle());
    }

    @Transactional(readOnly = true)
    public DriverProfileDTO getDriverProfileById(Long driverProfileId) {
        DriverProfile profile = findProfileOrThrow(driverProfileId);
        return toDTO(profile, profile.getVehicle());
    }

    // ─────────────────── Availability Toggle ───────────────────

    public DriverProfileDTO toggleAvailability(Long accountUserId) {
        DriverProfile profile = driverProfileRepository.findByAccountUserId(accountUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver profile not found for accountUserId: " + accountUserId));

        if (profile.getAvailabilityStatus() == AvailabilityStatus.ON_TRIP) {
            throw new BusinessException("Cannot change availability while on a trip.");
        }

        AvailabilityStatus newStatus = profile.getAvailabilityStatus() == AvailabilityStatus.ONLINE
                ? AvailabilityStatus.OFFLINE
                : AvailabilityStatus.ONLINE;

        profile.setAvailabilityStatus(newStatus);
        DriverProfile saved = driverProfileRepository.save(profile);
        log.info("Driver {} availability toggled to {}", accountUserId, newStatus);
        return toDTO(saved, saved.getVehicle());
    }

    /**
     * Called internally by ride-service via HTTP to set ON_TRIP / back to ONLINE.
     */
    public void updateAvailabilityStatus(Long driverProfileId, AvailabilityStatus status) {
        DriverProfile profile = findProfileOrThrow(driverProfileId);
        profile.setAvailabilityStatus(status);
        driverProfileRepository.save(profile);
        log.info("Driver profile {} status set to {}", driverProfileId, status);
    }

    // ─────────────────── Location Update ───────────────────

    public DriverProfileDTO updateLocation(Long accountUserId, LocationUpdateRequest request) {
        DriverProfile profile = driverProfileRepository.findByAccountUserId(accountUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver profile not found for accountUserId: " + accountUserId));

        profile.setCurrentLatitude(request.getLatitude());
        profile.setCurrentLongitude(request.getLongitude());
        DriverProfile saved = driverProfileRepository.save(profile);
        return toDTO(saved, saved.getVehicle());
    }

    // ─────────────────── Available Drivers (for ride-service) ───────────────────

    /**
     * Returns available ONLINE+verified drivers near the given coordinates.
     * Used by ride-service's OpenFeign client.
     */
    @Transactional(readOnly = true)
    public List<AvailableDriverDTO> getAvailableDriversNearby(double lat, double lng, double radiusKm) {
        List<DriverProfile> drivers = driverProfileRepository.findAvailableDriversNearby(lat, lng, radiusKm);

        return drivers.stream()
                .map(d -> {
                    double dist = haversineDistanceKm(lat, lng, d.getCurrentLatitude(), d.getCurrentLongitude());
                    Vehicle v = d.getVehicle();
                    String vehicleDesc = v != null ? v.getMake() + " " + v.getModel() : "Unknown";
                    String plate = v != null ? v.getPlateNumber() : "N/A";

                    return AvailableDriverDTO.builder()
                            .driverProfileId(d.getId())
                            .accountUserId(d.getAccountUserId())
                            .fullName(d.getFullName())
                            .plateNumber(plate)
                            .vehicleDescription(vehicleDesc)
                            .currentLatitude(d.getCurrentLatitude())
                            .currentLongitude(d.getCurrentLongitude())
                            .distanceKm(Math.round(dist * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─────────────────── Admin: Verify Driver ───────────────────

    public DriverProfileDTO verifyDriver(Long driverProfileId) {
        DriverProfile profile = findProfileOrThrow(driverProfileId);
        profile.setIsVerified(true);
        DriverProfile saved = driverProfileRepository.save(profile);
        log.info("Admin verified driver profile {}", driverProfileId);
        return toDTO(saved, saved.getVehicle());
    }

    // ─────────────────── Helpers ───────────────────

    private DriverProfile findProfileOrThrow(Long id) {
        return driverProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DriverProfile", id));
    }

    private DriverProfileDTO toDTO(DriverProfile profile, Vehicle vehicle) {
        DriverProfileDTO dto = DriverProfileDTO.builder()
                .id(profile.getId())
                .accountUserId(profile.getAccountUserId())
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .licenseNumber(profile.getLicenseNumber())
                .serviceArea(profile.getServiceArea())
                .availabilityStatus(profile.getAvailabilityStatus())
                .currentLatitude(profile.getCurrentLatitude())
                .currentLongitude(profile.getCurrentLongitude())
                .isVerified(profile.getIsVerified())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();

        if (vehicle != null) {
            dto.setVehicleId(vehicle.getId());
            dto.setPlateNumber(vehicle.getPlateNumber());
            dto.setMake(vehicle.getMake());
            dto.setModel(vehicle.getModel());
            dto.setYear(vehicle.getYear());
            dto.setColor(vehicle.getColor());
            dto.setVehicleType(vehicle.getVehicleType());
            dto.setCapacity(vehicle.getCapacity());
        }

        return dto;
    }

    /**
     * Haversine formula — calculates great-circle distance between two GPS points.
     */
    private double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
