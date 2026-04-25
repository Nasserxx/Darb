package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status recorded for a student's attendance at a session.
 */
@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PRESENT("present", "Present on time"),
    LATE("late", "Arrived late"),
    ABSENT("absent", "Did not attend"),
    EXCUSED("excused", "Excused absence"),
    HOLIDAY("holiday", "Session was a holiday");

    private final String key;
    private final String description;
}
