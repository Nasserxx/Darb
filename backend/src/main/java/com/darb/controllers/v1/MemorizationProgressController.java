package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.memorization.MemorizationProgressCreateRequest;
import com.darb.dtos.memorization.MemorizationProgressResponse;
import com.darb.dtos.memorization.MemorizationProgressUpdateRequest;
import com.darb.repositories.TeacherRepository;
import com.darb.services.MemorizationProgressService;

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
@RequestMapping("/api/v1/memorization")
@RequiredArgsConstructor
@Tag(name = "Memorization Progress", description = "Endpoints for tracking Quran memorization progress, recitation grades, and tajweed scores")
public class MemorizationProgressController {

    private final MemorizationProgressService memorizationProgressService;
    private final TeacherRepository teacherRepository;

    @GetMapping("/{id}")
    @Operation(
            summary = "Get memorization progress by ID",
            description = "Retrieves a specific memorization progress record by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Memorization progress retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Memorization progress not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MemorizationProgressResponse>> findById(
            @Parameter(description = "Memorization progress UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<MemorizationProgressResponse>builder()
                .success(true)
                .message("Memorization progress retrieved successfully")
                .data(memorizationProgressService.findById(id))
                .build());
    }

    @GetMapping("/student/{studentId}")
    @Operation(
            summary = "List memorization progress by student",
            description = "Returns a paginated list of memorization progress records for a specific student."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student memorization progress retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<MemorizationProgressResponse>>> findByStudent(
            @Parameter(description = "Student UUID", required = true) @PathVariable UUID studentId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MemorizationProgressResponse> page = memorizationProgressService.findByStudentId(studentId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<MemorizationProgressResponse>>builder()
                .success(true)
                .message("Student memorization progress retrieved successfully")
                .data(PageResponse.<MemorizationProgressResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/circle/{circleId}")
    @Operation(
            summary = "List memorization progress by circle",
            description = "Returns a paginated list of memorization progress records for all students in a specific circle."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circle memorization progress retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Circle not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<MemorizationProgressResponse>>> findByCircle(
            @Parameter(description = "Circle UUID", required = true) @PathVariable UUID circleId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MemorizationProgressResponse> page = memorizationProgressService.findByCircleId(circleId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<MemorizationProgressResponse>>builder()
                .success(true)
                .message("Circle memorization progress retrieved successfully")
                .data(PageResponse.<MemorizationProgressResponse>builder()
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
            summary = "Record memorization progress",
            description = "Creates a new memorization progress record with Surah range, grade, and tajweed score. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Memorization progress recorded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<MemorizationProgressResponse>> create(
            Authentication authentication,
            @Valid @RequestBody MemorizationProgressCreateRequest request) {
        UUID actorUserId = (UUID) authentication.getPrincipal();
        teacherRepository.findByUserId(actorUserId).stream().findFirst()
                .ifPresent(teacher -> request.setTeacherId(teacher.getId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MemorizationProgressResponse>builder()
                        .success(true)
                        .message("Memorization progress recorded successfully")
                        .data(memorizationProgressService.create(request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update memorization progress",
            description = "Updates an existing memorization progress record's grade, tajweed score, or teacher notes. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Memorization progress updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Memorization progress not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<MemorizationProgressResponse>> update(
            @Parameter(description = "Memorization progress UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody MemorizationProgressUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<MemorizationProgressResponse>builder()
                .success(true)
                .message("Memorization progress updated successfully")
                .data(memorizationProgressService.update(id, request))
                .build());
    }
}
