package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Grade assigned to a student's recitation session.
 */
@Getter
@RequiredArgsConstructor
public enum RecitationGrade {
    EXCELLENT("excellent", "Outstanding recitation", 5),
    VERY_GOOD("very_good", "Above average recitation", 4),
    GOOD("good", "Satisfactory recitation", 3),
    ACCEPTABLE("acceptable", "Needs improvement", 2),
    POOR("poor", "Below expected standard", 1);

    private final String key;
    private final String description;
    private final int numericValue;
}
