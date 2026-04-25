package com.darb.dtos.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Request body for creating a message")
public class MessageCreateRequest {

    @NotNull
    @Schema(description = "Sender user ID")
    private UUID senderId;

    @NotNull
    @Schema(description = "Receiver user ID")
    private UUID receiverId;

    @NotNull
    @Schema(description = "Circle ID (required by ER schema)")
    private UUID circleId;

    @NotBlank
    @Schema(description = "Message content", example = "Salam, please review the homework for tomorrow.")
    private String content;
}
