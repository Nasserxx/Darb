package com.darb.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body for refreshing JWT tokens")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "JWT refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
