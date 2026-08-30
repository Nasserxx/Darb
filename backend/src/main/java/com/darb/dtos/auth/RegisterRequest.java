package com.darb.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

import com.darb.entities.enums.Gender;
import com.darb.entities.enums.UserRole;

@Data
@Schema(description = "Request body for registering a new user")
public class RegisterRequest {

    @NotBlank @Size(max = 150)
    @Schema(description = "Full name", example = "Ahmed Al-Rashid", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @NotBlank @Email @Size(max = 255)
    @Schema(description = "Email address", example = "ahmed@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Size(max = 30)
    @Schema(description = "Phone number", example = "+966501234567")
    private String phone;

    @NotBlank @Size(min = 8, max = 128)
    @Schema(description = "Password (min 8 characters)", example = "P@ssw0rd123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "User role (student, teacher, parent, or mosque_admin; defaults to student)", example = "teacher")
    private UserRole role;

    @Schema(description = "Gender", example = "MALE")
    private Gender gender;

    @Schema(description = "Date of birth", example = "2000-01-15")
    private LocalDate dateOfBirth;

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

    @Size(max = 100)
    @Schema(description = "State / region", example = "Riyadh Province")
    private String addressState;
}
