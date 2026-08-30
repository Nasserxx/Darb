package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.user.UserPickerResponse;
import com.darb.dtos.user.UserResponse;
import com.darb.dtos.user.UserUpdateRequest;
import com.darb.entities.enums.UserRole;
import com.darb.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users, profiles, and account administration")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns a paginated list of all registered users. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponse> page = userService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserResponse>>builder()
                .success(true)
                .message("Users retrieved successfully")
                .data(PageResponse.<UserResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search users",
            description = "Searches users by name or email. Returns a paginated list matching the query. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameter"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            Authentication authentication,
            @Parameter(description = "Search query (name or email)", required = true)
            @RequestParam @NotBlank String q,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID callerId = (UUID) authentication.getPrincipal();
        Page<UserResponse> page = userService.searchUsers(callerId, q, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserResponse>>builder()
                .success(true)
                .message("Search results retrieved successfully")
                .data(PageResponse.<UserResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/picker")
    @Operation(
            summary = "Pick users for assignment",
            description = "Global user picker filtered by name, user address, and/or date of birth. "
                    + "Omits street and contact fields. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, TEACHER, and STUDENT."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Picker results retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid filter criteria"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<PageResponse<UserPickerResponse>>> pickUsers(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam(required = false) UUID mosqueId,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID callerId = (UUID) authentication.getPrincipal();
        Page<UserPickerResponse> page = userService.pickUsers(
                callerId, q, country, state, city, dateOfBirth, mosqueId, role, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserPickerResponse>>builder()
                .success(true)
                .message("Picker results retrieved successfully")
                .data(PageResponse.<UserPickerResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/picker/states")
    @Operation(
            summary = "List distinct user address states",
            description = "Distinct non-blank addressState values for users in the given country."
    )
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<String>>> listPickerStates(
            @RequestParam("country") String country) {
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .message("States retrieved successfully")
                .data(userService.listPickerStates(country))
                .build());
    }

    @GetMapping("/picker/cities")
    @Operation(
            summary = "List distinct user address cities",
            description = "Distinct non-blank city values for users in the given country (optional state)."
    )
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<String>>> listPickerCities(
            @RequestParam("country") String country,
            @RequestParam(value = "state", required = false) String state) {
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .message("Cities retrieved successfully")
                .data(userService.listPickerCities(country, state))
                .build());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user's details by their unique identifier. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> findById(
            Authentication authentication,
            @Parameter(description = "User UUID", required = true) @PathVariable UUID id) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User retrieved successfully")
                .data(userService.findById(callerId, id))
                .build());
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Returns the authenticated user's own profile information based on the JWT token."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Current user profile retrieved")
                .data(userService.findById(userId, userId))
                .build());
    }

    @PutMapping("/me")
    @Operation(
            summary = "Update own profile",
            description = "Allows the authenticated user to update their own profile fields such as name, phone, and avatar."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateOwnProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Profile updated successfully")
                .data(userService.update(userId, userId, request))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Admin update user",
            description = "Allows SUPER_ADMIN or MOSQUE_ADMIN (for users linked to their mosque) to update a user profile."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            Authentication authentication,
            @Parameter(description = "User UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User updated successfully")
                .data(userService.update(callerId, id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deactivate user",
            description = "Soft-deletes a user account by deactivating it. The user data is preserved but the account becomes inactive. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "User UUID", required = true) @PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User deactivated successfully")
                .build());
    }
}
