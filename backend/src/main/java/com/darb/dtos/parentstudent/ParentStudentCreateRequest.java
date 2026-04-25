package com.darb.dtos.parentstudent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request body for creating a parent-student relationship")
public class ParentStudentCreateRequest {

    @NotNull
    @Schema(description = "User ID of the parent", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID parentUserId;

    @NotNull
    @Schema(description = "Student ID to link", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID studentId;

    @Size(max = 50)
    @Schema(description = "Relationship to the student", example = "Father")
    private String relationship;

    @Schema(description = "Whether this is the primary parent/guardian", example = "true")
    private Boolean isPrimary;

    @Schema(description = "Whether the parent receives notifications", example = "true")
    private Boolean receivesNotifications;
}
