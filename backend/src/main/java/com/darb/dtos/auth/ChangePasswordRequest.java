package com.darb.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for changing user password")
public class ChangePasswordRequest {

    @NotBlank
    @Schema(description = "Current password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank @Size(min = 8, max = 128)
    @Schema(description = "New password (min 8 characters)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
