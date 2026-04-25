package com.darb.dtos.teacher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request body for creating a teacher")
public class TeacherCreateRequest {

    @NotNull
    @Schema(description = "User ID associated with this teacher", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID userId;

    @NotNull
    @Schema(description = "Mosque ID where the teacher is assigned", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID mosqueId;

    @Size(max = 200)
    @Schema(description = "Teacher specialization", example = "Tajweed & Hifz")
    private String specialization;

    @Schema(description = "Teacher biography", example = "Certified Quran teacher with 10 years of experience")
    private String bio;

    @Schema(description = "Years of teaching experience", example = "10")
    private Integer yearsExperience;

    @Size(max = 255)
    @Schema(description = "Ijazah chain description", example = "Hafs via Shatibiyyah")
    private String ijazahChain;
}
