package com.darb.dtos.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Join a mosque using an invite code")
public class InviteCodeJoinRequest {

    @NotBlank
    @Schema(description = "Role-specific invite code")
    private String inviteCode;
}
