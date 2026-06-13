package com.darb.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    public static Gender fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (Gender gender : values()) {
            if (gender.name().equalsIgnoreCase(value) || gender.key.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Invalid gender: " + value);
    }
}
