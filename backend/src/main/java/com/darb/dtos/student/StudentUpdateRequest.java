package com.darb.dtos.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a student")
public class StudentUpdateRequest {

    @Size(max = 100)
    @Schema(description = "Full name of the student")
    private String fullName;

    @Schema(description = "Medical notes about the student")
    private String medicalNotes;

    @Schema(description = "Number of memorized Juz", example = "5")
    private Integer memorizedJuz;

    @Size(max = 50)
    @Schema(description = "Parent invite code for linking parents to this student")
    private String parentInviteCode;
}
