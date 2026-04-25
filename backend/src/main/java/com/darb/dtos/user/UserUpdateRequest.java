package com.darb.dtos.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

import com.darb.entities.enums.Gender;

@Data
@Schema(description = "Request body for updating a user profile")
public class UserUpdateRequest {

    @Size(max = 150)
    @Schema(description = "Full name", example = "Ahmed Al-Rashid")
    private String fullName;

    @Schema(description = "Phone number", example = "+966501234567")
    private String phone;

    @Schema(description = "Gender", example = "MALE")
    private Gender gender;

    @Schema(description = "Date of birth", example = "2000-01-15")
    private LocalDate dateOfBirth;

    @Schema(description = "Avatar image URL")
    private String avatarUrl;
}
