package com.darb.controllers.v1;

import com.darb.dtos.achievement.AchievementCreateRequest;
import com.darb.dtos.achievement.AchievementResponse;
import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.services.AchievementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@Tag(name = "Achievement Management", description = "Endpoints for awarding and tracking student achievements and badges")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Get achievement by ID",
            description = "Retrieves a specific achievement by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Achievement retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Achievement not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AchievementResponse>> findById(
            @Parameter(description = "Achievement UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<AchievementResponse>builder()
                .success(true)
                .message("Achievement retrieved successfully")
                .data(achievementService.findById(id))
                .build());
    }

    @GetMapping("/student/{studentId}")
    @Operation(
            summary = "List achievements by student",
            description = "Returns a paginated list of achievements earned by a specific student."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student achievements retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<AchievementResponse>>> findByStudent(
            @Parameter(description = "Student UUID", required = true) @PathVariable UUID studentId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AchievementResponse> page = achievementService.findByStudentId(studentId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AchievementResponse>>builder()
                .success(true)
                .message("Student achievements retrieved successfully")
                .data(PageResponse.<AchievementResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/mosque/{mosqueId}")
    @Operation(
            summary = "List achievements by mosque",
            description = "Returns a paginated list of all achievements awarded within a specific mosque. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque achievements retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AchievementResponse>>> findByMosque(
            @Parameter(description = "Mosque UUID", required = true) @PathVariable UUID mosqueId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AchievementResponse> page = achievementService.findByMosqueId(mosqueId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AchievementResponse>>builder()
                .success(true)
                .message("Mosque achievements retrieved successfully")
                .data(PageResponse.<AchievementResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Award an achievement",
            description = "Creates and awards a new achievement to a student with a badge and description. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Achievement awarded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AchievementResponse>> create(
            Authentication authentication,
            @Valid @RequestBody AchievementCreateRequest request) {
        request.setAwardedBy((UUID) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AchievementResponse>builder()
                        .success(true)
                        .message("Achievement awarded successfully")
                        .data(achievementService.create(request))
                        .build());
    }
}
