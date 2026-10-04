package com.ridelink.fare.dto;

import com.ridelink.fare.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Official digital receipt issued upon successful payment")
public class ReceiptDTO {
    private Long receiptId;
    private String receiptNumber;
    private Long paymentId;
    private Long rideId;
    private Long passengerAccountId;
    private Long driverAccountId;
    private BigDecimal totalAmount;
    private String currency;
    private PaymentMethod paymentMethod;
    private LocalDateTime issuedAt;
    private String breakdownSummary;
}
