package com.darb.dtos.mosque;

import com.darb.validation.ValidTimeZone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a mosque")
public class MosqueUpdateRequest {

    @NotBlank @Size(max = 200)
    @Schema(description = "Mosque name")
    private String name;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Schema(description = "City")
    private String city;

    @Size(max = 100)
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a two-letter ISO code")
    @Schema(description = "Country (ISO 3166-1 alpha-2)")
    private String addressCountry;

    @Size(max = 20)
    @Schema(description = "Postal / ZIP code")
    private String addressPostalCode;

    @Size(max = 200)
    @Schema(description = "Street name")
    private String addressStreet;

    @Size(max = 20)
    @Schema(description = "House number")
    private String addressHouseNumber;

    @NotBlank(message = "State / Region is required")
    @Size(max = 100)
    @Schema(description = "State / region")
    private String addressState;

    @Size(max = 30)
    @Schema(description = "Contact phone")
    private String phone;

    @Size(max = 255)
    @Schema(description = "Contact email")
    private String email;

    @Schema(description = "Mosque logo URL")
    private String logoUrl;

    @ValidTimeZone
    @Schema(description = "Timezone (blank clears)")
    private String timezone;

    @Schema(description = "Mosque settings (JSON)")
    private String settings;
}
