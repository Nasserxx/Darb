package com.darb.dtos.workspace;

import com.darb.entities.enums.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resolved workspace profile for the authenticated user")
public class WorkspaceProfileResponse {

    @Schema(description = "Primary profile entity ID for this role")
    private UUID profileId;

    @Schema(description = "Assigned mosque ID, null for platform-wide roles")
    private UUID mosqueId;

    @Schema(description = "Teacher profile ID when role is TEACHER")
    private UUID teacherId;

    @Schema(description = "Student profile ID when role is STUDENT")
    private UUID studentId;

    @Schema(description = "Linked student IDs when role is PARENT")
    private List<UUID> parentStudentIds;

    @Schema(description = "Membership state for mosque-linked roles")
    private MembershipStatus membershipStatus;

    @Schema(description = "Assigned mosque name when membership is active")
    private String mosqueName;

    @Schema(description = "Mosque name when a join request is pending approval")
    private String pendingMosqueName;
}
