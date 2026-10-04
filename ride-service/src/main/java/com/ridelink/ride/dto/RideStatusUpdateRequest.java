package com.ridelink.ride.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request DTO for ride status transitions */
@Data
@Schema(description = "Request to update ride status")
public class RideStatusUpdateRequest {

    @Size(max = 500, message = "Cancellation reason too long")
    @Schema(description = "Required if transitioning to CANCELLED", example = "Passenger not available")
    private String cancellationReason;
}
