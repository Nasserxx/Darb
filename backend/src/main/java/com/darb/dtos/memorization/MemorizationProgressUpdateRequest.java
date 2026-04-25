package com.darb.dtos.memorization;

import com.darb.entities.enums.RecitationGrade;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a memorization progress record")
public class MemorizationProgressUpdateRequest {

    @Schema(description = "Grade assigned for this recitation")
    private RecitationGrade grade;

    @Schema(description = "Tajweed score (0-100)")
    private Integer tajweedScore;

    @Schema(description = "Teacher notes on the recitation")
    private String teacherNotes;

    @Schema(description = "URL to audio recording")
    private String audioUrl;
}
