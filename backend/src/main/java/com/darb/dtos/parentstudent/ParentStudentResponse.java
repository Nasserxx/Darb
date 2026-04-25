package com.darb.dtos.parentstudent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parent-student relationship response")
public class ParentStudentResponse {

    @Schema(description = "Unique relationship identifier")
    private UUID id;

    @Schema(description = "User ID of the parent")
    private UUID parentUserId;

    @Schema(description = "Student ID")
    private UUID studentId;

    @Schema(description = "Relationship to the student", example = "Father")
    private String relationship;

    @Schema(description = "Whether this is the primary parent/guardian")
    private Boolean isPrimary;

    @Schema(description = "Whether the parent receives notifications")
    private Boolean receivesNotifications;
}
