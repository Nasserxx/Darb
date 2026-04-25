package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Channel through which a notification is delivered.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationChannel {
    IN_APP("in_app", "In-application notification"),
    SMS("sms", "SMS message"),
    EMAIL("email", "Email message"),
    PUSH("push", "Push notification");

    private final String key;
    private final String description;
}
