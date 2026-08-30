package com.darb.dtos.mosque;

import com.darb.entities.enums.JoinRequestDirection;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.ParentRelationship;
import com.darb.entities.enums.UserRole;
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
@Schema(description = "A teacher or student join request awaiting mosque admin review")
public class MemberJoinRequestResponse {

    private UUID id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private UUID mosqueId;
    private String mosqueName;
    private UserRole requestedRole;
    private JoinRequestStatus status;
    private JoinRequestDirection direction;
    private UUID linkedStudentId;
    private ParentRelationship relationship;
    private Instant createdAt;
    private Instant reviewedAt;
}
