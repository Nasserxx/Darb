package com.darb.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lean user row for the assignment picker (no street or contact PII)")
public class UserPickerResponse {

    @Schema(description = "Unique user identifier")
    private UUID id;

    @Schema(description = "Full name", example = "Ahmed Al-Rashid")
    private String fullName;

    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;

    @Schema(description = "City", example = "Riyadh")
    private String city;

    @Schema(description = "Country (ISO 3166-1 alpha-2)", example = "SA")
    private String addressCountry;

    @Schema(description = "State / region", example = "Riyadh Province")
    private String addressState;
}
