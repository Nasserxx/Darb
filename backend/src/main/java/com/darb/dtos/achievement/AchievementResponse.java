package com.darb.dtos.achievement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.AchievementType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Achievement details response")
public class AchievementResponse {

    @Schema(description = "Unique achievement identifier")
    private UUID id;

    @Schema(description = "Student ID")
    private UUID studentId;

    @Schema(description = "Mosque ID")
    private UUID mosqueId;

    @Schema(description = "Type of achievement", example = "MEMORIZATION")
    private AchievementType type;

    @Schema(description = "Achievement title", example = "Completed Juz Amma")
    private String title;

    @Schema(description = "Achievement description")
    private String description;

    @Schema(description = "Badge image URL")
    private String badgeUrl;

    @Schema(description = "User ID who awarded the achievement")
    private UUID awardedBy;

    @Schema(description = "Date the achievement was awarded")
    private LocalDate awardedDate;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
