package com.ridelink.fare.dto;

import com.ridelink.fare.entity.PaymentMethod;
import com.ridelink.fare.entity.PaymentStatus;
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
@Schema(description = "Payment transaction status and response")
public class PaymentDTO {
    private Long paymentId;
    private Long fareId;
    private Long rideId;
    private Long passengerAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionReference;
    private String failureReason;
    private LocalDateTime paidAt;
    private String receiptNumber;
}
