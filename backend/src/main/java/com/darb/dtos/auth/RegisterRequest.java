package com.darb.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank @Email
    @Schema(description = "Email address", example = "ahmed@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Phone number", example = "+966501234567")
    private String phone;

    @NotBlank @Size(min = 8, max = 128)
    @Schema(description = "Password (min 8 characters)", example = "P@ssw0rd123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "User role", example = "STUDENT")
    private UserRole role;

    @Schema(description = "Gender", example = "MALE")
    private Gender gender;

    @Schema(description = "Date of birth", example = "2000-01-15")
    private LocalDate dateOfBirth;
}
