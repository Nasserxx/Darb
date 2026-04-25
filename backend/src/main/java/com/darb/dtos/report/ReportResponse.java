package com.darb.dtos.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.darb.entities.enums.ReportType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Report details response")
public class ReportResponse {

    @Schema(description = "Unique report identifier")
    private UUID id;

    @Schema(description = "Mosque ID")
    private UUID mosqueId;

    @Schema(description = "User ID who generated the report")
    private UUID generatedBy;

    @Schema(description = "Type of report", example = "ATTENDANCE_SUMMARY")
    private ReportType type;

    @Schema(description = "Report title", example = "April 2026 Attendance Summary")
    private String title;

    @Schema(description = "Report filters (JSON)")
    private String filters;

    @Schema(description = "Generated report file URL")
    private String fileUrl;

    @Schema(description = "Timestamp when the report was generated")
    private Instant generatedAt;
}
