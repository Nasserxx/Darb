package com.darb.dtos.memorization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.RecitationGrade;

@Data
@Schema(description = "Request body for creating a memorization progress record")
public class MemorizationProgressCreateRequest {

    @NotNull
    @Schema(description = "Student ID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
    private UUID studentId;

    @NotNull
    @Schema(description = "Circle ID")
    private UUID circleId;

    @NotNull
    @Schema(description = "Teacher ID who evaluated")
    private UUID teacherId;

    @NotNull
    @Schema(description = "Surah number (1-114)", example = "2")
    private Short surahNumber;

    @NotNull
    @Schema(description = "Starting Ayah number", example = "1")
    private Short ayahFrom;

    @NotNull
    @Schema(description = "Ending Ayah number", example = "25")
    private Short ayahTo;

    @Schema(description = "Grade assigned for this recitation", example = "VERY_GOOD")
    private RecitationGrade grade;

    @Schema(description = "Tajweed score (0-100)", example = "85")
    private Integer tajweedScore;

    @Schema(description = "Teacher notes on the recitation")
    private String teacherNotes;

    @Schema(description = "URL to audio recording")
    private String audioUrl;

    @NotNull
    @Schema(description = "Date of the session", example = "2026-04-20")
    private LocalDate sessionDate;
}
