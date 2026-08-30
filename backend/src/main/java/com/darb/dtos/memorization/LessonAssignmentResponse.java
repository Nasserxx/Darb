package com.darb.dtos.memorization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonAssignmentResponse {

    private UUID id;
    private UUID studentId;
    private UUID circleId;
    private List<String> halfPageIds;
    private String note;
    private UUID assignedBy;
    private Instant assignedAt;
}
