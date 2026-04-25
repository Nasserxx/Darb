package com.darb.dtos.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

import com.darb.entities.enums.NotificationChannel;
import com.darb.entities.enums.NotificationStatus;

@Data
@Schema(description = "Request body for creating a notification")
public class NotificationCreateRequest {

    @NotNull
    @Schema(description = "Mosque ID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
    private UUID mosqueId;

    @NotNull
    @Schema(description = "Recipient user ID")
    private UUID recipientUserId;

    @NotNull
    @Schema(description = "Sender user ID")
    private UUID senderUserId;

    @NotBlank @Size(max = 255)
    @Schema(description = "Notification title", example = "Session Cancelled")
    private String title;

    @NotBlank
    @Schema(description = "Notification body", example = "Tomorrow's circle session has been cancelled due to a holiday.")
    private String body;

    @NotNull
    @Schema(description = "Delivery channel", example = "IN_APP")
    private NotificationChannel channel;

    @Schema(description = "Notification status", example = "PENDING")
    private NotificationStatus status;

    @Schema(description = "Additional metadata (JSON)")
    private String metadata;
}
