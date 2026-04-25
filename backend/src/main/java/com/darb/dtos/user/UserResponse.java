package com.darb.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.Gender;
import com.darb.entities.enums.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User details response")
public class UserResponse {

    @Schema(description = "Unique user identifier")
    private UUID id;

    @Schema(description = "Full name", example = "Ahmed Al-Rashid")
    private String fullName;

    @Schema(description = "Email address", example = "ahmed@darb.app")
    private String email;

    @Schema(description = "Phone number", example = "+966501234567")
    private String phone;

    @Schema(description = "Assigned role", example = "STUDENT")
    private UserRole role;

    @Schema(description = "Gender")
    private Gender gender;

    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;

    @Schema(description = "Avatar image URL")
    private String avatarUrl;

    @Schema(description = "Whether the user account is active")
    private Boolean isActive;

    @Schema(description = "Timestamp of last login")
    private Instant lastLogin;

    @Schema(description = "Account creation timestamp")
    private Instant createdAt;
}
