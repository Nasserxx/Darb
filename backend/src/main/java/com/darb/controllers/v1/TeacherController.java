package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.InviteCodeJoinRequest;
import com.darb.dtos.common.MosqueIdRequest;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.teacher.TeacherCreateRequest;
import com.darb.dtos.teacher.TeacherResponse;
import com.darb.dtos.teacher.TeacherUpdateRequest;
import com.darb.services.TeacherService;

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
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher Management", description = "Endpoints for managing teacher profiles, assignments, and specializations")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @Operation(
            summary = "List all teachers",
            description = "Returns a paginated list of all teacher profiles. Accessible to any authenticated user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teachers retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<TeacherResponse>>> findAll(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TeacherResponse> page = teacherService.findAll((UUID) authentication.getPrincipal(), pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<TeacherResponse>>builder()
                .success(true)
                .message("Teachers retrieved successfully")
                .data(PageResponse.<TeacherResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get teacher by ID",
            description = "Retrieves a specific teacher's profile by their unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teacher retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Teacher not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TeacherResponse>> findById(
            Authentication authentication,
            @Parameter(description = "Teacher UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<TeacherResponse>builder()
                .success(true)
                .message("Teacher retrieved successfully")
                .data(teacherService.findById((UUID) authentication.getPrincipal(), id))
                .build());
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherResponse>> join(
            Authentication authentication,
            @Valid @RequestBody InviteCodeJoinRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<TeacherResponse>builder()
                        .success(true)
                        .message("Joined mosque successfully")
                        .data(teacherService.joinByInviteCode(userId, request.getInviteCode()))
                        .build());
    }

    @PostMapping("/onboard")
    @Operation(
            summary = "Self-onboard as a teacher",
            description = "Links the authenticated teacher user to a mosque. Rejects if a profile already exists."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Teacher onboarded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Wrong role or profile already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque not found")
    })
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherResponse>> onboard(
            Authentication authentication,
            @Valid @RequestBody MosqueIdRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<TeacherResponse>builder()
                        .success(true)
                        .message("Teacher onboarded successfully")
                        .data(teacherService.onboard(userId, request.getMosqueId()))
                        .build());
    }

    @PostMapping
    @Operation(
            summary = "Create a teacher profile",
            description = "Creates a new teacher profile linked to an existing user and mosque. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Teacher created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> create(
            Authentication authentication,
            @Valid @RequestBody TeacherCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<TeacherResponse>builder()
                        .success(true)
                        .message("Teacher created successfully")
                        .data(teacherService.create((UUID) authentication.getPrincipal(), request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a teacher profile",
            description = "Updates an existing teacher's specialization, bio, and availability. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teacher updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Teacher not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<TeacherResponse>> update(
            Authentication authentication,
            @Parameter(description = "Teacher UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody TeacherUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<TeacherResponse>builder()
                .success(true)
                .message("Teacher updated successfully")
                .data(teacherService.update((UUID) authentication.getPrincipal(), id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deactivate a teacher profile",
            description = "Soft-deletes a teacher profile by deactivating it. Only accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teacher deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Teacher not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication authentication,
            @Parameter(description = "Teacher UUID", required = true) @PathVariable UUID id) {
        teacherService.delete((UUID) authentication.getPrincipal(), id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Teacher deactivated successfully")
                .build());
    }
}
