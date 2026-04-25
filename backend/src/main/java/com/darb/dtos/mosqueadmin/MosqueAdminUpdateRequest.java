package com.darb.dtos.mosqueadmin;

import com.darb.entities.enums.AdminPermission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a mosque admin")
public class MosqueAdminUpdateRequest {

    @Schema(description = "Permission level for the admin", example = "MANAGE_STUDENTS")
    private AdminPermission permission;

    @Schema(description = "Whether this is the primary admin")
    private Boolean isPrimaryAdmin;
}
