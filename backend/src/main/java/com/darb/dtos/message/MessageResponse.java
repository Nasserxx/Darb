package com.darb.dtos.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.darb.entities.enums.MessageStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Message details response")
public class MessageResponse {

    @Schema(description = "Unique message identifier")
    private UUID id;

    @Schema(description = "Sender user ID")
    private UUID senderId;

    @Schema(description = "Sender full name")
    private String senderName;

    @Schema(description = "Receiver user ID")
    private UUID receiverId;

    @Schema(description = "Receiver full name")
    private String receiverName;

    @Schema(description = "Circle ID (for circle-threaded messages)")
    private UUID circleId;

    @Schema(description = "Circle name")
    private String circleName;

    @Schema(description = "Message content")
    private String content;

    @Schema(description = "Message status", example = "SENT")
    private MessageStatus status;

    @Schema(description = "Timestamp when sent")
    private Instant sentAt;

    @Schema(description = "Timestamp when read")
    private Instant readAt;
}
