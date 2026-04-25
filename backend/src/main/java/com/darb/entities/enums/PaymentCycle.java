package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Billing cycle for recurring payments.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentCycle {
    MONTHLY("monthly", "Monthly billing cycle"),
    QUARTERLY("quarterly", "Quarterly billing cycle"),
    SEMESTER("semester", "Semester-based billing"),
    ANNUAL("annual", "Annual billing cycle"),
    ONE_TIME("one_time", "One-time payment");

    private final String key;
    private final String description;
}
