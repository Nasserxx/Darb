package com.darb.dtos.circle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import com.darb.entities.enums.CircleLevel;
import com.darb.entities.enums.CircleStatus;
import com.darb.entities.enums.CircleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Study circle details response")
public class CircleResponse {

    @Schema(description = "Unique circle identifier")
    private UUID id;

    @Schema(description = "Mosque ID where the circle is held")
    private UUID mosqueId;

    @Schema(description = "Teacher ID who leads the circle")
    private UUID teacherId;

    @Schema(description = "Circle name", example = "Al-Baqarah Memorization Circle")
    private String name;

    @Schema(description = "Difficulty/progression level", example = "INTERMEDIATE")
    private CircleLevel level;

    @Schema(description = "Delivery type", example = "IN_PERSON")
    private CircleType type;

    @Schema(description = "Circle status", example = "ACTIVE")
    private CircleStatus status;

    @Schema(description = "Maximum number of students", example = "15")
    private Integer capacity;

    @Schema(description = "Session start time")
    private LocalTime startTime;

    @Schema(description = "Session end time")
    private LocalTime endTime;

    @Schema(description = "Days of the week", example = "SUN,TUE,THU")
    private String daysOfWeek;

    @Schema(description = "Room or meeting link", example = "Room 204")
    private String roomOrLink;

    @Schema(description = "Late threshold in minutes", example = "10")
    private Integer lateThresholdMinutes;

    @Schema(description = "Monthly fee")
    private BigDecimal monthlyFee;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
