package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Delivery mode of a study circle.
 */
@Getter
@RequiredArgsConstructor
public enum CircleType {
    IN_PERSON("in_person", "Physical attendance at mosque"),
    ONLINE("online", "Virtual/remote attendance"),
    HYBRID("hybrid", "Both in-person and online options");

    private final String key;
    private final String description;
}
