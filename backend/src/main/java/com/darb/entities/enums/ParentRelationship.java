package com.darb.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Relationship of a parent/guardian user to a linked student.
 */
@Getter
@RequiredArgsConstructor
public enum ParentRelationship {
    FATHER("father"),
    MOTHER("mother"),
    STEPFATHER("stepfather"),
    STEPMOTHER("stepmother"),
    GRANDFATHER("grandfather"),
    GRANDMOTHER("grandmother"),
    UNCLE("uncle"),
    AUNT("aunt"),
    BROTHER("brother"),
    SISTER("sister"),
    GUARDIAN("guardian"),
    PARENT("parent"),
    OTHER("other");

    private final String key;

    @JsonCreator
    public static ParentRelationship fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ParentRelationship relationship : values()) {
            if (relationship.name().equalsIgnoreCase(value) || relationship.key.equalsIgnoreCase(value)) {
                return relationship;
            }
        }
        throw new IllegalArgumentException("Invalid parent relationship: " + value);
    }
}
