package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Lifecycle status of a study circle.
 */
@Getter
@RequiredArgsConstructor
public enum CircleStatus {
    PLANNING("planning", "Being set up"),
    ACTIVE("active", "Currently running"),
    PAUSED("paused", "Temporarily suspended"),
    ENDED("ended", "Circle has concluded");

    private final String key;
    private final String description;
}
