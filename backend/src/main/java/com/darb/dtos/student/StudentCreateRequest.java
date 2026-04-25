package com.darb.dtos.student;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request body for creating a student")
public class StudentCreateRequest {

    @NotNull
    @Schema(description = "User ID associated with this student", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID userId;

    @NotNull
    @Schema(description = "Mosque ID where the student is enrolled", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID mosqueId;

    @Size(max = 50)
    @Schema(description = "National ID number", example = "1098765432")
    private String nationalId;

    @Schema(description = "Medical notes about the student")
    private String medicalNotes;

    @Schema(description = "Number of memorized Juz", example = "5")
    private Integer memorizedJuz;
}
