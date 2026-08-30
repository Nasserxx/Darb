package com.darb.dtos.parentstudent;

import com.darb.entities.enums.ParentRelationship;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request body for updating a parent-student relationship")
public class ParentStudentUpdateRequest {

    @Schema(description = "User ID of the parent", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID parentUserId;

    @Schema(description = "Student ID to link", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID studentId;

    @Schema(description = "Relationship to the student", example = "MOTHER")
    private ParentRelationship relationship;

    @Schema(description = "Whether this is the primary parent/guardian")
    private Boolean isPrimary;

    @Schema(description = "Whether the parent receives notifications")
    private Boolean receivesNotifications;

    @Schema(description = "Audit reason for super admin override (min 8 characters)")
    private String auditReason;
}
