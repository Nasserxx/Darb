package com.darb.dtos.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

import com.darb.entities.enums.ReportType;

@Data
@Schema(description = "Request body for creating a report")
public class ReportCreateRequest {

    @NotNull
    @Schema(description = "Mosque ID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID mosqueId;

    @NotNull
    @Schema(description = "User ID who generates the report")
    private UUID generatedBy;

    @NotNull
    @Schema(description = "Type of report", example = "ATTENDANCE_SUMMARY")
    private ReportType type;

    @NotBlank @Size(max = 255)
    @Schema(description = "Report title", example = "April 2026 Attendance Summary")
    private String title;

    @Schema(description = "Report filters (JSON)", example = "{\"month\": \"2026-04\", \"circleId\": \"...\"}")
    private String filters;

    @Schema(description = "Generated report file URL")
    private String fileUrl;
}
