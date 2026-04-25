package com.darb.dtos.parentstudent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a parent-student relationship")
public class ParentStudentUpdateRequest {

    @Size(max = 50)
    @Schema(description = "Relationship to the student", example = "Father")
    private String relationship;

    @Schema(description = "Whether this is the primary parent/guardian")
    private Boolean isPrimary;

    @Schema(description = "Whether the parent receives notifications")
    private Boolean receivesNotifications;
}
