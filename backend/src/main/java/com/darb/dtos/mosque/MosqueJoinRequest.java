package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for joining a mosque via invite code")
public class MosqueJoinRequest {

    @NotBlank
    @Size(min = 8, max = 32)
    @Schema(description = "Admin invite code shared by the mosque founder", example = "aB3xK9mNpQ2r")
    private String inviteCode;
}
