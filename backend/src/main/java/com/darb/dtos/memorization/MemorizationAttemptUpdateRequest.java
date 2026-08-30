package com.darb.dtos.memorization;

import com.darb.entities.enums.RecitationGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorizationAttemptUpdateRequest {

    private RecitationGrade grade;
    private String notes;
}
