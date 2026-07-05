package com.darb.dtos.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request body with a mosque identifier")
public class MosqueIdRequest {

    @NotNull
    @Schema(description = "Mosque UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID mosqueId;
}
