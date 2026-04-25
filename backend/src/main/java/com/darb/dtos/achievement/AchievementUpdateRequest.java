package com.darb.dtos.achievement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating an achievement")
public class AchievementUpdateRequest {

    @Size(max = 200)
    @Schema(description = "Achievement title")
    private String title;

    @Schema(description = "Achievement description")
    private String description;

    @Schema(description = "Badge image URL")
    private String badgeUrl;
}
