package com.ridelink.account.entity;

/**
 * Account lifecycle status.
 * ACTIVE    — normal operation
 * SUSPENDED — temporarily blocked
 * DELETED   — soft-deleted (not physically removed)
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
