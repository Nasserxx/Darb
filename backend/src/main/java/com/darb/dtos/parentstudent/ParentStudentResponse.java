package com.darb.dtos.parentstudent;

import com.darb.entities.enums.ParentRelationship;
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

    @Schema(description = "Full name of the parent")
    private String parentName;

    @Schema(description = "Student ID")
    private UUID studentId;

    @Schema(description = "Full name of the student")
    private String studentName;

    @Schema(description = "Relationship to the student", example = "FATHER")
    private ParentRelationship relationship;

    @Schema(description = "Whether this is the primary parent/guardian")
    private Boolean isPrimary;

    @Schema(description = "Whether the parent receives notifications")
    private Boolean receivesNotifications;
}
