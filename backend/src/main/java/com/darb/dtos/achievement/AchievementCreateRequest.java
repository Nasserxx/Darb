package com.darb.dtos.achievement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.AchievementType;

@Data
@Schema(description = "Request body for creating an achievement")
public class AchievementCreateRequest {

    @NotNull
    @Schema(description = "Student ID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
    private UUID studentId;

    @NotNull
    @Schema(description = "Mosque ID")
    private UUID mosqueId;

    @NotNull
    @Schema(description = "Type of achievement", example = "MEMORIZATION")
    private AchievementType type;

    @NotBlank @Size(max = 200)
    @Schema(description = "Achievement title", example = "Completed Juz Amma")
    private String title;

    @Schema(description = "Achievement description", example = "Student has memorized all of Juz 30")
    private String description;

    @Schema(description = "Badge image URL")
    private String badgeUrl;

    @Schema(description = "User ID who awarded the achievement")
    private UUID awardedBy;

    @Schema(description = "Date the achievement was awarded", example = "2026-04-20")
    private LocalDate awardedDate;
}
