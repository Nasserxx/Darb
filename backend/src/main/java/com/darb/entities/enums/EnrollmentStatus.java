package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Status of a student's enrollment in a study circle.
 */
@Getter
@RequiredArgsConstructor
public enum EnrollmentStatus {
    PENDING("pending", "Awaiting approval"),
    ACTIVE("active", "Currently enrolled"),
    WITHDRAWN("withdrawn", "Withdrawn from circle"),
    COMPLETED("completed", "Completed the circle program"),
    REJECTED("rejected", "Enrollment was rejected");

    private final String key;
    private final String description;
}
