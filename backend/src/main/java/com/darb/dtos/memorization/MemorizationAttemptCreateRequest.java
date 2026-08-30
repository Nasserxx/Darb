package com.darb.dtos.memorization;

import com.darb.entities.enums.PageHalf;
import com.darb.entities.enums.RecitationGrade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorizationAttemptCreateRequest {

    @NotNull
    private UUID circleId;

    @NotNull
    @Min(1)
    @Max(604)
    private Short page;

    @NotNull
    private PageHalf half;

    @NotNull
    private LocalDate sessionDate;

    private RecitationGrade grade;

    private String notes;

    @Valid
    @Size(max = 50)
    @Builder.Default
    private List<StampDto> stamps = new ArrayList<>();
}
