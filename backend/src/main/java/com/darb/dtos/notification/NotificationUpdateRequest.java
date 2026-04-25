package com.darb.dtos.notification;

import com.darb.entities.enums.NotificationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a notification")
public class NotificationUpdateRequest {

    @Schema(description = "Notification status")
    private NotificationStatus status;

    @Schema(description = "Additional metadata (JSON)")
    private String metadata;
}
