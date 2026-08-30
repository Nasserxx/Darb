package com.darb.dtos.memorization;

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
public class MemorizationCoverageResponse {

    private String grain;
    private UUID studentId;
    private UUID circleId;
    private List<CoverageItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverageItem {
        private String key;
        private int assessed;
        private int total;
        private Double progressPercent;
    }
}
