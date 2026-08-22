package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Preview of a mosque matched by invite code")
public class MosqueJoinPreviewResponse {

    @Schema(description = "Mosque name for user confirmation", example = "Al-Noor Mosque")
    private String mosqueName;

    @Schema(description = "Country (ISO 3166-1 alpha-2)")
    private String addressCountry;

    @Schema(description = "Postal / ZIP code")
    private String addressPostalCode;

    @Schema(description = "Street name")
    private String addressStreet;

    @Schema(description = "House number")
    private String addressHouseNumber;

    @Schema(description = "State / region")
    private String addressState;
}
