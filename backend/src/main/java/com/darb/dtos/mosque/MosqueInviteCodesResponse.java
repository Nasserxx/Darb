package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invite codes for mosque members (mosque admin only)")
public class MosqueInviteCodesResponse {

    @Schema(description = "Admin co-admin invite code")
    private String adminInviteCode;

    @Schema(description = "Teacher invite code")
    private String teacherInviteCode;

    @Schema(description = "Student invite code")
    private String studentInviteCode;
}
