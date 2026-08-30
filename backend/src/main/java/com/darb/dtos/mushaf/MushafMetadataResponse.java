package com.darb.dtos.mushaf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MushafMetadataResponse {

    private String edition;
    private int pageCount;
    private int juzCount;
    private int halfCount;
    private List<Integer> juzStartPages;
    private List<HalfPageEntry> halves;
}
