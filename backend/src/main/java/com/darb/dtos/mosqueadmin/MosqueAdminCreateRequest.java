package com.darb.dtos.mosqueadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

import com.darb.entities.enums.AdminPermission;

@Data
@Schema(description = "Request body for assigning a mosque admin")
public class MosqueAdminCreateRequest {

    @NotNull
    @Schema(description = "User ID to assign as admin", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID userId;

    @NotNull
    @Schema(description = "Mosque ID to administer", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID mosqueId;

    @NotNull
    @Schema(description = "Permission level for the admin", example = "FULL_ACCESS")
    private AdminPermission permission;

    @Schema(description = "Whether this is the primary admin", example = "false")
    private Boolean isPrimaryAdmin;

    @Schema(description = "User ID who is assigning this admin")
    private UUID assignedBy;
}
