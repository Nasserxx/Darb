package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of reports that can be generated within the system.
 */
@Getter
@RequiredArgsConstructor
public enum ReportType {
    ATTENDANCE_SUMMARY("attendance_summary", "Attendance statistics"),
    FINANCIAL("financial", "Financial report"),
    STUDENT_PROGRESS("student_progress", "Student memorization progress"),
    TEACHER_PERFORMANCE("teacher_performance", "Teacher evaluation report"),
    ENROLLMENT("enrollment", "Enrollment statistics");

    private final String key;
    private final String description;
}
