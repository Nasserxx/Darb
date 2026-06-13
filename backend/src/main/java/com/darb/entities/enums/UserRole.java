package com.darb.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the roles available to users within the Darb platform.
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {
    SUPER_ADMIN("super_admin", "Full platform administrator"),
    MOSQUE_ADMIN("mosque_admin", "Mosque-level administrator"),
    TEACHER("teacher", "Circle teacher/instructor"),
    STUDENT("student", "Enrolled student"),
    PARENT("parent", "Parent or guardian of a student");

    private final String key;
    private final String description;

    @JsonCreator
    public static UserRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(value) || role.key.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role: " + value);
    }
}
