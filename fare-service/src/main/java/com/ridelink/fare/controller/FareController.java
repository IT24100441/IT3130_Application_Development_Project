package com.ridelink.fare.controller;

import com.ridelink.fare.dto.*;
import com.ridelink.fare.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
@Tag(name = "Fare Management", description = "Fare estimation and completion pricing calculation")
@SecurityRequirement(name = "bearerAuth")
public class FareController {

    private final FareService fareService;

    @PostMapping("/estimate")
    @Operation(summary = "Estimate fare", description = "Calculate estimated trip fare before requesting a ride.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fare estimated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<FareEstimateDTO> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareService.estimateFare(request));
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate final fare", description = "Calculates final fare upon ride completion (Called by Ride Management Service or client).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Final fare calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<FareDTO> calculateFare(@Valid @RequestBody FareCalculationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fareService.calculateFinalFare(request));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get fare by ride ID", description = "Retrieve fare details calculated for a specific ride.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fare found"),
            @ApiResponse(responseCode = "404", description = "Fare record not found")
    })
    public ResponseEntity<FareDTO> getFareByRideId(@PathVariable Long rideId) {
        return ResponseEntity.ok(fareService.getFareByRideId(rideId));
    }

    @GetMapping("/{fareId}")
    @Operation(summary = "Get fare by ID", description = "Retrieve fare record by its primary key.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fare found"),
            @ApiResponse(responseCode = "404", description = "Fare record not found")
    })
    public ResponseEntity<FareDTO> getFareById(@PathVariable Long fareId) {
        return ResponseEntity.ok(fareService.getFareById(fareId));
    }
}
