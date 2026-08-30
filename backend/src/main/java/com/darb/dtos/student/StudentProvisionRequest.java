package com.darb.dtos.student;

import com.darb.entities.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Schema(description = "Create a student login and seat them immediately")
public class StudentProvisionRequest {

    @NotNull
    private UUID mosqueId;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;

    @Size(max = 100)
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a two-letter ISO code")
    private String addressCountry;

    @Size(max = 100)
    private String addressState;

    @Size(max = 100)
    private String city;

    @Size(max = 20)
    private String addressPostalCode;

    @Size(max = 200)
    private String addressStreet;

    @Size(max = 20)
    private String addressHouseNumber;

    private String medicalNotes;
    private Integer memorizedJuz;
}
