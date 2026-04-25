package com.darb.dtos.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.GoalStatus;

@Data
@Schema(description = "Request body for creating a goal")
public class GoalCreateRequest {

    @NotNull
    @Schema(description = "Student ID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
    private UUID studentId;

    @NotNull
    @Schema(description = "Circle ID")
    private UUID circleId;

    @NotBlank @Size(max = 200)
    @Schema(description = "Goal title", example = "Memorize Surah Al-Mulk")
    private String title;

    @Schema(description = "Target Surah number (1-114)", example = "67")
    private Short targetSurah;

    @Schema(description = "Target Juz number (1-30)", example = "29")
    private Short targetJuz;

    @Schema(description = "Goal status", example = "IN_PROGRESS")
    private GoalStatus status;

    @Schema(description = "Due date for the goal", example = "2026-06-30")
    private LocalDate dueDate;

    @Schema(description = "User ID who set the goal")
    private UUID setBy;
}
