package com.darb.dtos.mosque;

import com.darb.validation.ValidTimeZone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request body for creating a mosque")
public class MosqueCreateRequest {

    @NotBlank @Size(max = 200)
    @Schema(description = "Mosque name", example = "Al-Noor Mosque")
    private String name;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Schema(description = "City", example = "Riyadh")
    private String city;

    @Size(max = 100)
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a two-letter ISO code")
    @Schema(description = "Country (ISO 3166-1 alpha-2)", example = "SA")
    private String addressCountry;

    @Size(max = 20)
    @Schema(description = "Postal / ZIP code", example = "12211")
    private String addressPostalCode;

    @Size(max = 200)
    @Schema(description = "Street name", example = "King Fahd Road")
    private String addressStreet;

    @Size(max = 20)
    @Schema(description = "House number", example = "123")
    private String addressHouseNumber;

    @NotBlank(message = "State / Region is required")
    @Size(max = 100)
    @Schema(description = "State / region", example = "Riyadh Province")
    private String addressState;

    @Size(max = 30)
    @Schema(description = "Contact phone", example = "+966112345678")
    private String phone;

    @Size(max = 255)
    @Schema(description = "Contact email", example = "info@alnoor-mosque.sa")
    private String email;

    @Schema(description = "Mosque logo URL")
    private String logoUrl;

    @ValidTimeZone
    @Schema(description = "IANA timezone", example = "Asia/Riyadh")
    private String timezone;
}
