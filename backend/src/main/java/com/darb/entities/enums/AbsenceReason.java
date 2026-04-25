package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Reason given for a student's absence.
 */
@Getter
@RequiredArgsConstructor
public enum AbsenceReason {
    SICK("sick", "Student was sick"),
    FAMILY("family", "Family emergency"),
    TRAVEL("travel", "Traveling"),
    PERSONAL("personal", "Personal reasons"),
    OTHER("other", "Other reason");

    private final String key;
    private final String description;
}
