package com.darb.dtos.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Whether the operation succeeded", example = "true")
    private boolean success;

    @Schema(description = "Human-readable message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response payload")
    private T data;
}
