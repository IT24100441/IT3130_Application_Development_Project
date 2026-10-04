package com.ridelink.ride.controller;

import com.ridelink.ride.dto.*;
import com.ridelink.ride.entity.RideStatus;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

/**
 * Ride Management Controller.
 *
 * Design note: The JWT credentials field (password) carries the Long userId
 * (set in JwtAuthFilter). This avoids a cross-service call to account-service
 * just to resolve the user's ID.
 */
@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
@Tag(name = "Ride Management", description = "Create rides, manage state transitions, view history")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Request a new ride",
            description = "Creates a new ride and automatically assigns the nearest available driver. " +
                          "Requires PASSENGER JWT.")
    public ResponseEntity<RideDTO> createRide(
            Authentication auth,
            @Valid @RequestBody RideRequestDTO request) {

        Long passengerId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rideService.createRide(passengerId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ride by ID")
    public ResponseEntity<RideDTO> getRideById(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.getRideById(id));
    }

    @GetMapping("/my-rides")
    @Operation(summary = "Get my ride history",
            description = "Returns the authenticated user's rides (paginated). " +
                          "Works for both PASSENGER and DRIVER roles.")
    public ResponseEntity<Page<RideDTO>> getMyRides(
            Authentication auth,
            @PageableDefault(size = 10) Pageable pageable) {

        Long userId  = (Long) auth.getCredentials();
        boolean isDriver = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_DRIVER"));
        return ResponseEntity.ok(rideService.getMyRides(userId, isDriver, pageable));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "[DRIVER] Accept assigned ride",
            description = "Transitions: ASSIGNED → ACCEPTED. Only the assigned driver can do this.")
    public ResponseEntity<RideDTO> acceptRide(@PathVariable Long id, Authentication auth) {
        Long driverId = (Long) auth.getCredentials();
        return ResponseEntity.ok(rideService.updateRideStatus(
                id, RideStatus.ACCEPTED, driverId, true, null));
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "[DRIVER] Start ride (pick up passenger)",
            description = "Transitions: ACCEPTED → IN_PROGRESS.")
    public ResponseEntity<RideDTO> startRide(@PathVariable Long id, Authentication auth) {
        Long driverId = (Long) auth.getCredentials();
        return ResponseEntity.ok(rideService.updateRideStatus(
                id, RideStatus.IN_PROGRESS, driverId, true, null));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "[DRIVER] Complete ride",
            description = "Transitions: IN_PROGRESS → COMPLETED. " +
                          "Triggers fare calculation in fare-service.")
    public ResponseEntity<RideDTO> completeRide(@PathVariable Long id, Authentication auth) {
        Long driverId = (Long) auth.getCredentials();
        return ResponseEntity.ok(rideService.updateRideStatus(
                id, RideStatus.COMPLETED, driverId, true, null));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel ride",
            description = "Cancels from any non-terminal state. " +
                          "Can be done by the passenger or assigned driver.")
    public ResponseEntity<RideDTO> cancelRide(
            @PathVariable Long id,
            Authentication auth,
            @Valid @RequestBody(required = false) RideStatusUpdateRequest request) {

        Long actorId = (Long) auth.getCredentials();
        boolean isDriver = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_DRIVER"));
        return ResponseEntity.ok(rideService.updateRideStatus(
                id, RideStatus.CANCELLED, actorId, isDriver, request));
    }
}
