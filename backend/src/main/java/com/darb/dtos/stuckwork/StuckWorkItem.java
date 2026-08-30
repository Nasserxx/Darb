package com.darb.dtos.stuckwork;

import com.darb.entities.enums.JoinRequestDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cross-mosque stuck work item for SUPER_ADMIN fleet hub")
public class StuckWorkItem {

    private StuckWorkItemKind kind;
    private JoinRequestDirection direction;
    private UUID mosqueId;
    private String mosqueName;
    private UUID resourceId;
    private String summary;
    private Instant createdAt;
    private int densityScore;
}
