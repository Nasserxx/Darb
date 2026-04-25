package com.darb.dtos.memorization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.RecitationGrade;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Memorization progress record response")
public class MemorizationProgressResponse {

    @Schema(description = "Unique progress record identifier")
    private UUID id;

    @Schema(description = "Student ID")
    private UUID studentId;

    @Schema(description = "Circle ID")
    private UUID circleId;

    @Schema(description = "Teacher ID who evaluated")
    private UUID teacherId;

    @Schema(description = "Surah number", example = "2")
    private Short surahNumber;

    @Schema(description = "Starting Ayah number")
    private Short ayahFrom;

    @Schema(description = "Ending Ayah number")
    private Short ayahTo;

    @Schema(description = "Grade assigned", example = "VERY_GOOD")
    private RecitationGrade grade;

    @Schema(description = "Tajweed score (0-100)", example = "85")
    private Integer tajweedScore;

    @Schema(description = "Teacher notes")
    private String teacherNotes;

    @Schema(description = "Audio recording URL")
    private String audioUrl;

    @Schema(description = "Session date")
    private LocalDate sessionDate;

    @Schema(description = "Record creation timestamp")
    private Instant createdAt;
}
