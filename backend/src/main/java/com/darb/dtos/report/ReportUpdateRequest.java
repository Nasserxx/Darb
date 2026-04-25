package com.darb.dtos.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a report")
public class ReportUpdateRequest {

    @Size(max = 255)
    @Schema(description = "Report title")
    private String title;

    @Schema(description = "Report filters (JSON)")
    private String filters;

    @Schema(description = "Generated report file URL")
    private String fileUrl;
}
