package com.darb.dtos.teacher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Teacher details response")
public class TeacherResponse {

    @Schema(description = "Unique teacher identifier")
    private UUID id;

    @Schema(description = "Associated user ID")
    private UUID userId;

    @Schema(description = "Assigned mosque ID")
    private UUID mosqueId;

    @Schema(description = "Teacher specialization", example = "Tajweed & Hifz")
    private String specialization;

    @Schema(description = "Teacher biography")
    private String bio;

    @Schema(description = "Years of teaching experience", example = "10")
    private Integer yearsExperience;

    @Schema(description = "Ijazah chain description")
    private String ijazahChain;

    @Schema(description = "Whether the teacher is available for assignment")
    private Boolean isAvailable;

    @Schema(description = "Whether the teacher profile is active")
    private Boolean isActive;

    @Schema(description = "Timestamp when the teacher joined")
    private Instant joinedAt;
}
