package com.darb.dtos.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.GoalStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Goal details response")
public class GoalResponse {

    @Schema(description = "Unique goal identifier")
    private UUID id;

    @Schema(description = "Student ID")
    private UUID studentId;

    @Schema(description = "Circle ID")
    private UUID circleId;

    @Schema(description = "Goal title", example = "Memorize Surah Al-Mulk")
    private String title;

    @Schema(description = "Target Surah number")
    private Short targetSurah;

    @Schema(description = "Target Juz number")
    private Short targetJuz;

    @Schema(description = "Goal status", example = "IN_PROGRESS")
    private GoalStatus status;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Date the goal was completed")
    private LocalDate completedDate;

    @Schema(description = "User ID who set the goal")
    private UUID setBy;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
