package com.darb.dtos.attendance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.darb.entities.enums.AbsenceReason;
import com.darb.entities.enums.AttendanceStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Attendance record response")
public class AttendanceResponse {

    @Schema(description = "Unique attendance record identifier")
    private UUID id;

    @Schema(description = "Enrollment ID")
    private UUID enrollmentId;

    @Schema(description = "Circle ID")
    private UUID circleId;

    @Schema(description = "Date of the session")
    private LocalDate sessionDate;

    @Schema(description = "Attendance status", example = "PRESENT")
    private AttendanceStatus status;

    @Schema(description = "Scheduled session start time")
    private LocalTime scheduledStart;

    @Schema(description = "Actual check-in time")
    private LocalTime actualCheckIn;

    @Schema(description = "Minutes late (0 if on time)")
    private Integer minutesLate;

    @Schema(description = "Whether the parent was notified")
    private Boolean parentNotified;

    @Schema(description = "Reason for absence")
    private AbsenceReason absenceReason;

    @Schema(description = "URL for excuse document")
    private String excuseDocumentUrl;

    @Schema(description = "User ID who recorded the attendance")
    private UUID recordedBy;

    @Schema(description = "Record creation timestamp")
    private Instant createdAt;
}
