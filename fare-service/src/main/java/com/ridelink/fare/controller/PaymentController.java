package com.ridelink.fare.controller;

import com.ridelink.fare.dto.PaymentDTO;
import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Simulated payment processing, status tracking, and receipts")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Process simulated payment", description = "Charges the passenger for a completed ride and generates a receipt.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "409", description = "Payment already processed for this ride"),
            @ApiResponse(responseCode = "422", description = "Simulated payment failure (e.g. card declined)")
    })
    public ResponseEntity<PaymentDTO> processPayment(@Valid @RequestBody PaymentRequest request,
                                                     Authentication authentication) {
        Long passengerId = (Long) authentication.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(request, passengerId));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment transaction details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get payment by ride ID", description = "Retrieve payment transaction associated with a ride.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<PaymentDTO> getPaymentByRideId(@PathVariable Long rideId) {
        return ResponseEntity.ok(paymentService.getPaymentByRideId(rideId));
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Get passenger payment history", description = "Retrieve all payments made by the authenticated passenger.")
    public ResponseEntity<List<PaymentDTO>> getMyPayments(Authentication authentication) {
        Long passengerId = (Long) authentication.getCredentials();
        return ResponseEntity.ok(paymentService.getPassengerPayments(passengerId));
    }
}
