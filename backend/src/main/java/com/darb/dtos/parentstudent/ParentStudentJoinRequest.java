package com.darb.dtos.parentstudent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for a parent to join via invite code")
public class ParentStudentJoinRequest {

    @NotBlank
    @Size(min = 8, max = 50)
    @Schema(description = "Parent invite code provided by the mosque admin", example = "abc123def")
    private String inviteCode;
}
