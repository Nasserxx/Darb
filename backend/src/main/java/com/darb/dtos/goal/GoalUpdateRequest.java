package com.darb.dtos.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

import com.darb.entities.enums.GoalStatus;

@Data
@Schema(description = "Request body for updating a goal")
public class GoalUpdateRequest {

    @Size(max = 200)
    @Schema(description = "Goal title")
    private String title;

    @Schema(description = "Target Surah number (1-114)")
    private Short targetSurah;

    @Schema(description = "Target Juz number (1-30)")
    private Short targetJuz;

    @Schema(description = "Goal status")
    private GoalStatus status;

    @Schema(description = "Due date for the goal")
    private LocalDate dueDate;

    @Schema(description = "Date the goal was completed")
    private LocalDate completedDate;
}
