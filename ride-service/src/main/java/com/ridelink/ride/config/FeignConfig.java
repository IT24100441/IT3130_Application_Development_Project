package com.ridelink.ride.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign global configuration.
 * - Propagates the Authorization header from incoming requests to outgoing Feign calls.
 * - Configures a custom error decoder for readable error messages.
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * Propagates the JWT Bearer token to downstream microservice calls.
     * Enables driver-service and fare-service to validate the caller's identity.
     */
    @Bean
    public RequestInterceptor jwtPropagationInterceptor() {
        return requestTemplate -> {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                String authHeader = servletAttributes.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    log.debug("Propagating JWT to downstream service: {}", requestTemplate.url());
                }
            }
        };
    }

    /**
     * Custom Feign error decoder — logs and converts HTTP errors from downstream services.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign error calling {}: HTTP {}", methodKey, response.status());
            return switch (response.status()) {
                case 404 -> new com.ridelink.ride.exception.ResourceNotFoundException(
                        "Downstream service resource not found (404) calling: " + methodKey);
                case 409 -> new com.ridelink.ride.exception.BusinessException(
                        "Downstream service conflict (409) calling: " + methodKey);
                default -> new com.ridelink.ride.exception.ServiceCommunicationException(
                        "Error calling downstream service [" + methodKey + "]: HTTP " + response.status());
            };
        };
    }
}
