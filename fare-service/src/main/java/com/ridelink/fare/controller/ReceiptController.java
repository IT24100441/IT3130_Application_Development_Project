package com.ridelink.fare.controller;

import com.ridelink.fare.dto.ReceiptDTO;
import com.ridelink.fare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipt Management", description = "Digital receipt retrieval and history")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final PaymentService paymentService;

    @GetMapping("/number/{receiptNumber}")
    @Operation(summary = "Get receipt by receipt number", description = "Retrieve digital receipt by unique receipt number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt found"),
            @ApiResponse(responseCode = "404", description = "Receipt not found")
    })
    public ResponseEntity<ReceiptDTO> getReceiptByNumber(@PathVariable String receiptNumber) {
        return ResponseEntity.ok(paymentService.getReceiptByReceiptNumber(receiptNumber));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get receipt by ride ID", description = "Retrieve digital receipt issued for a specific ride.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt found"),
            @ApiResponse(responseCode = "404", description = "Receipt not found")
    })
    public ResponseEntity<ReceiptDTO> getReceiptByRideId(@PathVariable Long rideId) {
        return ResponseEntity.ok(paymentService.getReceiptByRideId(rideId));
    }

    @GetMapping("/my-receipts")
    @PreAuthorize("hasRole('PASSENGER')")
    @Operation(summary = "Get passenger receipts", description = "Retrieve all receipts issued to the authenticated passenger.")
    public ResponseEntity<List<ReceiptDTO>> getMyReceipts(Authentication authentication) {
        Long passengerId = (Long) authentication.getCredentials();
        return ResponseEntity.ok(paymentService.getPassengerReceipts(passengerId));
    }
}
