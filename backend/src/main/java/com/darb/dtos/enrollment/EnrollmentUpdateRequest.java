package com.darb.dtos.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

import com.darb.entities.enums.EnrollmentStatus;

@Data
@Schema(description = "Request body for updating an enrollment")
public class EnrollmentUpdateRequest {

    @Schema(description = "Enrollment status")
    private EnrollmentStatus status;

    @Schema(description = "Date of withdrawal", example = "2026-03-01")
    private LocalDate withdrawnDate;

    @Schema(description = "Additional notes")
    private String notes;
}
