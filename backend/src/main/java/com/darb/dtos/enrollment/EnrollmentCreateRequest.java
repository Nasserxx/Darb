package com.darb.dtos.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.EnrollmentStatus;

@Data
@Schema(description = "Request body for creating an enrollment")
public class EnrollmentCreateRequest {

    @NotNull
    @Schema(description = "Student ID to enroll", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
    private UUID studentId;

    @NotNull
    @Schema(description = "Circle ID to enroll into", example = "d4e5f6a7-b8c9-0123-defa-234567890123")
    private UUID circleId;

    @Schema(description = "Enrollment status", example = "PENDING")
    private EnrollmentStatus status;

    @Schema(description = "Enrollment date", example = "2026-01-15")
    private LocalDate enrolledDate;

    @Schema(description = "User ID who approves the enrollment")
    private UUID approvedBy;

    @Schema(description = "Additional notes")
    private String notes;
}
