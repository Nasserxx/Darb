package com.darb.dtos.memorization;

import com.darb.entities.enums.PageHalf;
import com.darb.entities.enums.RecitationGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorizationAttemptResponse {

    private UUID id;
    private UUID studentId;
    private UUID circleId;
    private Short page;
    private PageHalf half;
    private String edition;
    private LocalDate sessionDate;
    private UUID assessorId;
    private RecitationGrade grade;
    private String notes;
    private List<StampDto> stamps;
    private Integer tajweedCount;
    private Integer hifzCount;
    private Instant createdAt;
    private Instant updatedAt;
}
