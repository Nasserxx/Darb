package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Delivery status of a notification.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationStatus {
    PENDING("pending", "Queued for delivery"),
    SENT("sent", "Delivered to channel"),
    DELIVERED("delivered", "Confirmed delivery"),
    FAILED("failed", "Delivery failed"),
    READ("read", "Read by recipient");

    private final String key;
    private final String description;
}
