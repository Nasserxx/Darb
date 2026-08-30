package com.darb.services;

import com.darb.dtos.auth.*;
import com.darb.entities.RefreshTokenHash;
import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.DuplicateResourceException;
import com.darb.exceptions.UnauthorizedException;
import com.darb.repositories.RefreshTokenHashRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenHashRepository refreshTokenHashRepository;

    private static final java.util.Set<UserRole> SELF_REGISTRATION_ROLES =
            java.util.Set.of(UserRole.STUDENT, UserRole.TEACHER, UserRole.PARENT, UserRole.MOSQUE_ADMIN);

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        UserRole role = resolveRegistrationRole(request.getRole());

        User user = User.builder()
                .fullName(request.getFullName())
                .email(email)
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .city(request.getCity())
                .addressCountry(request.getAddressCountry())
                .addressPostalCode(request.getAddressPostalCode())
                .addressStreet(request.getAddressStreet())
                .addressHouseNumber(request.getAddressHouseNumber())
                .addressState(request.getAddressState())
                .isActive(true)
                .build();

        userRepository.save(user);
    }

    private UserRole resolveRegistrationRole(UserRole requested) {
        UserRole role = requested != null ? requested : UserRole.STUDENT;
        if (!SELF_REGISTRATION_ROLES.contains(role)) {
            throw new BadRequestException(
                    "Role not allowed for self-registration. Allowed roles: student, teacher, parent, mosque_admin");
        }
        return role;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Kill all prior refresh hashes before minting a new session
        revokeAllRefreshTokens(user.getId());

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        AuthResponse response = generateAuthResponse(user);

        String tokenHash = sha256Hex(response.getRefreshToken());
        refreshTokenHashRepository.save(RefreshTokenHash.builder()
                .user(user)
                .tokenHash(tokenHash)
                .build());

        return response;
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.isTokenValid(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (!"refresh".equals(jwtService.getTokenTypeFromToken(request.getRefreshToken()))) {
            throw new UnauthorizedException("Invalid refresh token type");
        }

        // Consume the old refresh token (rotation)
        String tokenHash = sha256Hex(request.getRefreshToken());
        RefreshTokenHash storedHash = refreshTokenHashRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
        refreshTokenHashRepository.delete(storedHash);

        var userId = jwtService.getUserIdFromToken(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        AuthResponse response = generateAuthResponse(user);

        // Store hash of the new refresh token
        String newHash = sha256Hex(response.getRefreshToken());
        refreshTokenHashRepository.save(RefreshTokenHash.builder()
                .user(user)
                .tokenHash(newHash)
                .build());

        return response;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        revokeAllRefreshTokens(userId);
    }

    @Transactional
    public void logout(UUID userId) {
        revokeAllRefreshTokens(userId);
    }

    private void revokeAllRefreshTokens(UUID userId) {
        refreshTokenHashRepository.deleteByUserId(userId);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .userId(user.getId())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }
}
