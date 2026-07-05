package com.darb.dtos.mosque;

import com.darb.dtos.mosqueadmin.MosqueAdminResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after mosque admin onboarding (create or join)")
public class MosqueOnboardResponse {

    @Schema(description = "The mosque the user was linked to")
    private MosqueResponse mosque;

    @Schema(description = "The mosque admin assignment created for the user")
    private MosqueAdminResponse admin;

    @Schema(description = "Invite code for co-admins (returned only when creating a new mosque)")
    private String inviteCode;

    @Schema(description = "Teacher invite code (returned only when creating a new mosque)")
    private String teacherInviteCode;

    @Schema(description = "Student invite code (returned only when creating a new mosque)")
    private String studentInviteCode;
}
