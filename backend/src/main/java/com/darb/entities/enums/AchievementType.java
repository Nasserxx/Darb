package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Category of achievement awarded to a student.
 */
@Getter
@RequiredArgsConstructor
public enum AchievementType {
    MEMORIZATION("memorization", "Quran memorization milestone"),
    ATTENDANCE("attendance", "Perfect attendance streak"),
    RECITATION("recitation", "Outstanding recitation"),
    COMPETITION("competition", "Competition winner"),
    MILESTONE("milestone", "General milestone achievement");

    private final String key;
    private final String description;
}
