package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the gender of a user.
 */
@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("male"),
    FEMALE("female");

    private final String key;
}
