package com.darb.dtos.student;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.darb.entities.enums.EnrollmentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Student details response")
public class StudentResponse {

    @Schema(description = "Unique student identifier")
    private UUID id;

    @Schema(description = "Associated user ID")
    private UUID userId;

    @Schema(description = "Enrolled mosque ID")
    private UUID mosqueId;

    @Schema(description = "National ID number", example = "1098765432")
    private String nationalId;

    @Schema(description = "Medical notes")
    private String medicalNotes;

    @Schema(description = "Number of memorized Juz", example = "5")
    private Integer memorizedJuz;

    @Schema(description = "Total absences count", example = "3")
    private Integer totalAbsences;

    @Schema(description = "Total late arrivals count", example = "1")
    private Integer totalLateArrivals;

    @Schema(description = "Enrollment status", example = "ACTIVE")
    private EnrollmentStatus status;

    @Schema(description = "Timestamp when the student enrolled")
    private Instant enrolledAt;
}
