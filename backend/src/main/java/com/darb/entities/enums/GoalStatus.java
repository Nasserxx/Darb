package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status of a student's memorization or learning goal.
 */
@Getter
@RequiredArgsConstructor
public enum GoalStatus {
    IN_PROGRESS("in_progress", "Currently working towards"),
    COMPLETED("completed", "Goal achieved"),
    OVERDUE("overdue", "Past due date"),
    CANCELLED("cancelled", "Goal was cancelled");

    private final String key;
    private final String description;
}
