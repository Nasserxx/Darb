package com.darb.dtos.mushaf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HalfPageEntry {

    private String id;
    private String edition;
    private int page;
    private String half;
    private int juz;
    private int surahFrom;
    private int ayahFrom;
    private int surahTo;
    private int ayahTo;
}
