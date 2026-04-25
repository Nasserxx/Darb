package com.darb.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response with JWT tokens and user info")
public class AuthResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access token expiry in seconds", example = "900")
    private Long expiresIn;

    @Schema(description = "Authenticated user ID")
    private UUID userId;

    @Schema(description = "User full name", example = "Ahmed Al-Rashid")
    private String fullName;

    @Schema(description = "User role", example = "MOSQUE_ADMIN")
    private String role;

    @Schema(description = "User email", example = "admin@darb.app")
    private String email;
}
