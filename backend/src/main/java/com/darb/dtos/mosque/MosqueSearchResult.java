package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Limited mosque info for member search")
public class MosqueSearchResult {

    private UUID id;
    private String name;
    private String city;
    private String addressCountry;
    private String addressPostalCode;
    private String addressStreet;
    private String addressHouseNumber;
    private String addressState;
}
