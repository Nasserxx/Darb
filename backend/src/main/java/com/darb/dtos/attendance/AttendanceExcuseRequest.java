package com.darb.dtos.attendance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for submitting an absence excuse")
public class AttendanceExcuseRequest {

    @Size(max = 500)
    @Schema(description = "Reason for the absence", example = "Was feeling unwell")
    private String absenceReason;

    @Size(max = 500)
    @Schema(description = "URL to an excuse document (e.g., medical certificate)")
    private String excuseDocumentUrl;
}
