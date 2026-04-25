package com.darb.dtos.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.EnrollmentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Enrollment details response")
public class EnrollmentResponse {

    @Schema(description = "Unique enrollment identifier")
    private UUID id;

    @Schema(description = "Student ID")
    private UUID studentId;

    @Schema(description = "Circle ID")
    private UUID circleId;

    @Schema(description = "Enrollment status", example = "ACTIVE")
    private EnrollmentStatus status;

    @Schema(description = "Enrollment date")
    private LocalDate enrolledDate;

    @Schema(description = "Withdrawal date")
    private LocalDate withdrawnDate;

    @Schema(description = "User ID who approved the enrollment")
    private UUID approvedBy;

    @Schema(description = "Additional notes")
    private String notes;
}
