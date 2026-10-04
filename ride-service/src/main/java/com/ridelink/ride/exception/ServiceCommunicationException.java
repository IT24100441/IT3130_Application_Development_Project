package com.ridelink.ride.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when an inter-service HTTP call fails unexpectedly */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceCommunicationException extends RuntimeException {
    public ServiceCommunicationException(String message) { super(message); }
}
