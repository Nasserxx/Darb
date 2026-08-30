package com.darb.dtos.memorization;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonAssignmentUpsertRequest {

    @NotNull
    private UUID circleId;

    @NotEmpty
    private List<String> halfPageIds;

    private String note;
}
