package com.ridelink.fare.dto;

import com.ridelink.fare.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to process a simulated payment for a completed ride")
public class PaymentRequest {

    @NotNull(message = "rideId is required")
    @Schema(description = "ID of the completed ride to pay for", example = "101")
    private Long rideId;

    @NotNull(message = "paymentMethod is required")
    @Schema(description = "Payment method: CASH, CREDIT_CARD, DEBIT_CARD, DIGITAL_WALLET", example = "CREDIT_CARD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Optional card number for simulation (Card ending in '0002' triggers simulated card decline)", example = "4111222233334444")
    private String cardNumber;

    @Schema(description = "Optional CVV for simulation", example = "123")
    private String cvv;

    @Schema(description = "Client idempotency key to prevent double charging", example = "idem-abc-12345")
    private String idempotencyKey;
}
