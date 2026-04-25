package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.enrollment.EnrollmentCreateRequest;
import com.darb.dtos.enrollment.EnrollmentResponse;
import com.darb.dtos.enrollment.EnrollmentUpdateRequest;
import com.darb.services.EnrollmentService;

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
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Management", description = "Endpoints for managing student enrollments in study circles")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @Operation(
            summary = "List all enrollments",
            description = "Returns a paginated list of all enrollments. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<EnrollmentResponse> page = enrollmentService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<EnrollmentResponse>>builder()
                .success(true)
                .message("Enrollments retrieved successfully")
                .data(PageResponse.<EnrollmentResponse>builder()
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
            summary = "Get enrollment by ID",
            description = "Retrieves a specific enrollment record by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> findById(
            @Parameter(description = "Enrollment UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<EnrollmentResponse>builder()
                .success(true)
                .message("Enrollment retrieved successfully")
                .data(enrollmentService.findById(id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Create an enrollment",
            description = "Enrolls a student into a study circle with a specified status. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Enrollment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> create(
            Authentication authentication,
            @Valid @RequestBody EnrollmentCreateRequest request) {
        request.setApprovedBy((UUID) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<EnrollmentResponse>builder()
                        .success(true)
                        .message("Enrollment created successfully")
                        .data(enrollmentService.create(request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update an enrollment",
            description = "Updates an enrollment's status, withdrawal date, or notes. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> update(
            @Parameter(description = "Enrollment UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody EnrollmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<EnrollmentResponse>builder()
                .success(true)
                .message("Enrollment updated successfully")
                .data(enrollmentService.update(id, request))
                .build());
    }
}
