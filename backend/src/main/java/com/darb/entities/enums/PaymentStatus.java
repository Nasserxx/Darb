package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status of a payment within the system.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("pending", "Awaiting payment"),
    PARTIAL("partial", "Partially paid"),
    PAID("paid", "Fully paid"),
    OVERDUE("overdue", "Past due date"),
    WAIVED("waived", "Payment waived"),
    REFUNDED("refunded", "Payment refunded");

    private final String key;
    private final String description;
}
