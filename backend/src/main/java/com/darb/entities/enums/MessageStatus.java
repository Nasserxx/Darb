package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status of a direct message between users.
 */
@Getter
@RequiredArgsConstructor
public enum MessageStatus {
    SENT("sent", "Message sent"),
    DELIVERED("delivered", "Message delivered"),
    READ("read", "Message read by recipient");

    private final String key;
    private final String description;
}
