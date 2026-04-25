package com.darb.dtos.circle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import com.darb.entities.enums.CircleLevel;
import com.darb.entities.enums.CircleStatus;
import com.darb.entities.enums.CircleType;

@Data
@Schema(description = "Request body for creating a study circle")
public class CircleCreateRequest {

    @NotNull
    @Schema(description = "Mosque ID where the circle is held", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID mosqueId;

    @NotNull
    @Schema(description = "Teacher ID who leads the circle", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
    private UUID teacherId;

    @NotBlank @Size(max = 200)
    @Schema(description = "Circle name", example = "Al-Baqarah Memorization Circle")
    private String name;

    @NotNull
    @Schema(description = "Difficulty/progression level", example = "INTERMEDIATE")
    private CircleLevel level;

    @NotNull
    @Schema(description = "Delivery type (in-person, online, hybrid)", example = "IN_PERSON")
    private CircleType type;

    @Schema(description = "Circle status", example = "PLANNING")
    private CircleStatus status;

    @Schema(description = "Maximum number of students", example = "15")
    private Integer capacity;

    @Schema(description = "Session start time", example = "16:00")
    private LocalTime startTime;

    @Schema(description = "Session end time", example = "18:00")
    private LocalTime endTime;

    @Size(max = 100)
    @Schema(description = "Days of the week (comma-separated)", example = "SUN,TUE,THU")
    private String daysOfWeek;

    @Schema(description = "Physical room or online meeting link", example = "Room 204")
    private String roomOrLink;

    @Schema(description = "Minutes late before marking as late", example = "10")
    private Integer lateThresholdMinutes;

    @Schema(description = "Monthly fee for the circle", example = "150.00")
    private BigDecimal monthlyFee;
}
