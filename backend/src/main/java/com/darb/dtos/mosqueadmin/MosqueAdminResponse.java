package com.darb.dtos.mosqueadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.darb.entities.enums.AdminPermission;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mosque admin details response")
public class MosqueAdminResponse {

    @Schema(description = "Unique mosque admin assignment identifier")
    private UUID id;

    @Schema(description = "Associated user ID")
    private UUID userId;

    @Schema(description = "Administered mosque ID")
    private UUID mosqueId;

    @Schema(description = "Permission level", example = "FULL_ACCESS")
    private AdminPermission permission;

    @Schema(description = "Whether this is the primary admin")
    private Boolean isPrimaryAdmin;

    @Schema(description = "Timestamp when the admin was assigned")
    private Instant assignedAt;

    @Schema(description = "User ID who assigned this admin")
    private UUID assignedBy;
}
