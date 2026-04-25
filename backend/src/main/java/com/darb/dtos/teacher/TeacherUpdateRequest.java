package com.darb.dtos.teacher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a teacher")
public class TeacherUpdateRequest {

    @Size(max = 200)
    @Schema(description = "Teacher specialization", example = "Tajweed & Hifz")
    private String specialization;

    @Schema(description = "Teacher biography")
    private String bio;

    @Schema(description = "Years of teaching experience", example = "10")
    private Integer yearsExperience;

    @Size(max = 255)
    @Schema(description = "Ijazah chain description")
    private String ijazahChain;

    @Schema(description = "Whether the teacher is available for assignment")
    private Boolean isAvailable;
}
