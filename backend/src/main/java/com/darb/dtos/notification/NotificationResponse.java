package com.darb.dtos.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.darb.entities.enums.NotificationChannel;
import com.darb.entities.enums.NotificationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification details response")
public class NotificationResponse {

    @Schema(description = "Unique notification identifier")
    private UUID id;

    @Schema(description = "Mosque ID")
    private UUID mosqueId;

    @Schema(description = "Recipient user ID")
    private UUID recipientUserId;

    @Schema(description = "Sender user ID")
    private UUID senderUserId;

    @Schema(description = "Notification title", example = "Session Cancelled")
    private String title;

    @Schema(description = "Notification body")
    private String body;

    @Schema(description = "Delivery channel", example = "IN_APP")
    private NotificationChannel channel;

    @Schema(description = "Notification status", example = "PENDING")
    private NotificationStatus status;

    @Schema(description = "Timestamp when sent")
    private Instant sentAt;

    @Schema(description = "Timestamp when delivered")
    private Instant deliveredAt;

    @Schema(description = "Additional metadata (JSON)")
    private String metadata;
}
