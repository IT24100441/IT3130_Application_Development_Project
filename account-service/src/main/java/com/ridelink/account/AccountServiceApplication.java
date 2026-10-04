package com.ridelink.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Account Service — Entry Point
 * Handles: user registration, login, JWT issuance, profile management.
 * Port: 8081
 */
@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
