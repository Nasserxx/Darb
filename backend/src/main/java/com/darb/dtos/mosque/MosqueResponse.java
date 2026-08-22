package com.darb.dtos.mosque;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mosque details response")
public class MosqueResponse {

    @Schema(description = "Unique mosque identifier")
    private UUID id;

    @Schema(description = "Mosque name", example = "Al-Noor Mosque")
    private String name;

    @Schema(description = "City")
    private String city;

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

    @Schema(description = "Contact phone")
    private String phone;

    @Schema(description = "Contact email")
    private String email;

    @Schema(description = "Logo URL")
    private String logoUrl;

    @Schema(description = "Timezone")
    private String timezone;

    @Schema(description = "Mosque settings (JSON)")
    private String settings;

    @Schema(description = "Whether the mosque is active")
    private Boolean isActive;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
