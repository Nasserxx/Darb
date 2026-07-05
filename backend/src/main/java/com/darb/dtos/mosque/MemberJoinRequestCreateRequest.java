package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to join a mosque pending admin approval")
public class MemberJoinRequestCreateRequest {

    @NotNull
    @Schema(description = "Target mosque ID")
    private UUID mosqueId;
}
