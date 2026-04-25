package com.darb.dtos.circle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.darb.entities.enums.CircleLevel;
import com.darb.entities.enums.CircleStatus;
import com.darb.entities.enums.CircleType;

@Data
@Schema(description = "Request body for updating a study circle")
public class CircleUpdateRequest {

    @Size(max = 200)
    @Schema(description = "Circle name")
    private String name;

    @Schema(description = "Difficulty/progression level")
    private CircleLevel level;

    @Schema(description = "Delivery type")
    private CircleType type;

    @Schema(description = "Circle status")
    private CircleStatus status;

    @Schema(description = "Maximum number of students")
    private Integer capacity;

    @Schema(description = "Session start time")
    private LocalTime startTime;

    @Schema(description = "Session end time")
    private LocalTime endTime;

    @Size(max = 100)
    @Schema(description = "Days of the week (comma-separated)")
    private String daysOfWeek;

    @Schema(description = "Physical room or online meeting link")
    private String roomOrLink;

    @Schema(description = "Minutes late before marking as late")
    private Integer lateThresholdMinutes;

    @Schema(description = "Monthly fee for the circle")
    private BigDecimal monthlyFee;
}
