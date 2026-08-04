package com.darb.dtos.parentstudent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Preview of the student and mosque for a parent invite code")
public class ParentStudentJoinPreviewResponse {

    @Schema(description = "Mosque name")
    private String mosqueName;

    @Schema(description = "Student full name")
    private String studentName;
}
