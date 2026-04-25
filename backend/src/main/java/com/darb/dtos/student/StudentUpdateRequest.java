package com.darb.dtos.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a student")
public class StudentUpdateRequest {

    @Size(max = 50)
    @Schema(description = "National ID number")
    private String nationalId;

    @Schema(description = "Medical notes about the student")
    private String medicalNotes;

    @Schema(description = "Number of memorized Juz", example = "5")
    private Integer memorizedJuz;

    @Schema(description = "Total absences count", example = "3")
    private Integer totalAbsences;

    @Schema(description = "Total late arrivals count", example = "1")
    private Integer totalLateArrivals;
}
