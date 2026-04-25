package com.darb.dtos.message;

import com.darb.entities.enums.MessageStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a message")
public class MessageUpdateRequest {

    @Schema(description = "Message status", example = "READ")
    private MessageStatus status;
}
