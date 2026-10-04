package com.ridelink.driver.controller;

import com.ridelink.driver.dto.*;
import com.ridelink.driver.entity.AvailabilityStatus;
import com.ridelink.driver.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Driver & Vehicle controller.
 *
 * Principal strategy:
 *   - The JWT credential (principal) is the email string.
 *   - The JWT credential password field holds the Long userId.
 *   - Role is extracted from authorities.
 */
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver & Vehicle", description = "Driver profiles, vehicles, availability, and location")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;
    private final com.ridelink.driver.security.JwtUtil jwtUtil;

    // ─────── Registration ───────

    @PostMapping("/register")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Register driver profile + vehicle",
            description = "Creates a new driver profile linked to the authenticated DRIVER account. " +
                          "Requires a DRIVER role JWT.")
    public ResponseEntity<DriverProfileDTO> registerProfile(
            Authentication auth,
            @Valid @RequestBody DriverRegistrationRequest request) {

        Long accountUserId = (Long) auth.getCredentials();
        String email = auth.getName();
        // fullName stored in auth would require DB — for demo we use email as fullName placeholder
        DriverProfileDTO dto = driverService.registerDriverProfile(
                accountUserId, email, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ─────── Own Profile ───────

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get my driver profile")
    public ResponseEntity<DriverProfileDTO> getMyProfile(Authentication auth) {
        Long accountUserId = (Long) auth.getCredentials();
        return ResponseEntity.ok(driverService.getDriverProfileByAccountId(accountUserId));
    }

    // ─────── Availability Toggle ───────

    @PostMapping("/toggle-availability")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Toggle ONLINE/OFFLINE status",
            description = "Switches the driver's availability between ONLINE and OFFLINE. " +
                          "Cannot toggle while ON_TRIP.")
    public ResponseEntity<DriverProfileDTO> toggleAvailability(Authentication auth) {
        Long accountUserId = (Long) auth.getCredentials();
        return ResponseEntity.ok(driverService.toggleAvailability(accountUserId));
    }

    // ─────── Location Update ───────

    @PutMapping("/location")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update current GPS location",
            description = "Updates the driver's simulated current location. Call when ONLINE.")
    public ResponseEntity<DriverProfileDTO> updateLocation(
            Authentication auth,
            @Valid @RequestBody LocationUpdateRequest request) {
        Long accountUserId = (Long) auth.getCredentials();
        return ResponseEntity.ok(driverService.updateLocation(accountUserId, request));
    }

    // ─────── Available Drivers Query (used by ride-service) ───────

    @GetMapping("/available")
    @Operation(summary = "Find available drivers nearby",
            description = "Returns ONLINE+verified drivers within the given radius. " +
                          "Called by ride-service OpenFeign client.")
    public ResponseEntity<List<AvailableDriverDTO>> getAvailableDrivers(
            @Parameter(description = "Pickup latitude",  example = "6.9271") @RequestParam double lat,
            @Parameter(description = "Pickup longitude", example = "79.8612") @RequestParam double lng,
            @Parameter(description = "Search radius in km", example = "10.0")
            @RequestParam(defaultValue = "10.0") double radius) {

        return ResponseEntity.ok(driverService.getAvailableDriversNearby(lat, lng, radius));
    }

    // ─────── By Profile ID (used by ride-service) ───────

    @GetMapping("/by-profile/{driverProfileId}")
    @Operation(summary = "Get driver profile by profile ID",
            description = "Used by ride-service to fetch driver details for assignment.")
    public ResponseEntity<DriverProfileDTO> getByProfileId(
            @PathVariable Long driverProfileId) {
        return ResponseEntity.ok(driverService.getDriverProfileById(driverProfileId));
    }

    // ─────── Internal: Update Status (called by ride-service) ───────

    @PatchMapping("/internal/{driverProfileId}/status")
    @Operation(summary = "[INTERNAL] Update driver availability status",
            description = "Called by ride-service to set ON_TRIP or revert to ONLINE.")
    public ResponseEntity<Void> updateDriverStatus(
            @PathVariable Long driverProfileId,
            @RequestParam AvailabilityStatus status) {
        driverService.updateAvailabilityStatus(driverProfileId, status);
        return ResponseEntity.ok().build();
    }

    // ─────── Admin ───────

    @PatchMapping("/admin/{driverProfileId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Verify a driver profile",
            description = "Marks the driver as verified, making them eligible for ride assignments.")
    public ResponseEntity<DriverProfileDTO> verifyDriver(@PathVariable Long driverProfileId) {
        return ResponseEntity.ok(driverService.verifyDriver(driverProfileId));
    }
}
