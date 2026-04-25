package com.darb.dtos.attendance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalTime;

import com.darb.entities.enums.AbsenceReason;
import com.darb.entities.enums.AttendanceStatus;

@Data
@Schema(description = "Request body for updating an attendance record")
public class AttendanceUpdateRequest {

    @Schema(description = "Attendance status")
    private AttendanceStatus status;

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
}
