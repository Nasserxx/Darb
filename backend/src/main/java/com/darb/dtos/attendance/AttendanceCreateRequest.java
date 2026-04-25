package com.darb.dtos.attendance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.darb.entities.enums.AbsenceReason;
import com.darb.entities.enums.AttendanceStatus;

@Data
@Schema(description = "Request body for creating an attendance record")
public class AttendanceCreateRequest {

    @NotNull
    @Schema(description = "Enrollment ID for this attendance record")
    private UUID enrollmentId;

    @NotNull
    @Schema(description = "Circle ID for the session")
    private UUID circleId;

    @NotNull
    @Schema(description = "Date of the session", example = "2026-04-20")
    private LocalDate sessionDate;

    @NotNull
    @Schema(description = "Attendance status", example = "PRESENT")
    private AttendanceStatus status;

    @Schema(description = "Scheduled session start time", example = "16:00")
    private LocalTime scheduledStart;

    @Schema(description = "Actual check-in time", example = "16:05")
    private LocalTime actualCheckIn;

    @Schema(description = "Minutes late (0 if on time)", example = "5")
    private Integer minutesLate;

    @Schema(description = "Whether the parent was notified of absence", example = "false")
    private Boolean parentNotified;

    @Schema(description = "Reason for absence", example = "SICK")
    private AbsenceReason absenceReason;

    @Schema(description = "URL for excuse document")
    private String excuseDocumentUrl;

    @Schema(description = "User ID who recorded the attendance")
    private UUID recordedBy;
}
