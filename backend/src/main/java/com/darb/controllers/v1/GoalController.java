package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.goal.GoalCreateRequest;
import com.darb.dtos.goal.GoalResponse;
import com.darb.dtos.goal.GoalUpdateRequest;
import com.darb.services.GoalService;

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
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Goal Management", description = "Endpoints for setting and tracking Quran memorization goals for students")
public class GoalController {

    private final GoalService goalService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Get goal by ID",
            description = "Retrieves a specific goal by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Goal retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Goal not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<GoalResponse>> findById(
            @Parameter(description = "Goal UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<GoalResponse>builder()
                .success(true)
                .message("Goal retrieved successfully")
                .data(goalService.findById(id))
                .build());
    }

    @GetMapping("/student/{studentId}")
    @Operation(
            summary = "List goals by student",
            description = "Returns a paginated list of goals for a specific student."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student goals retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<GoalResponse>>> findByStudent(
            @Parameter(description = "Student UUID", required = true) @PathVariable UUID studentId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GoalResponse> page = goalService.findByStudentId(studentId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<GoalResponse>>builder()
                .success(true)
                .message("Student goals retrieved successfully")
                .data(PageResponse.<GoalResponse>builder()
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
            summary = "Create a goal",
            description = "Creates a new memorization goal for a student with target Surah/Juz and due date. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Goal created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<GoalResponse>> create(
            Authentication authentication,
            @Valid @RequestBody GoalCreateRequest request) {
        request.setSetBy((UUID) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<GoalResponse>builder()
                        .success(true)
                        .message("Goal created successfully")
                        .data(goalService.create(request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a goal",
            description = "Updates an existing goal's title, status, target, or due date. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Goal updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Goal not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<GoalResponse>> update(
            @Parameter(description = "Goal UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody GoalUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<GoalResponse>builder()
                .success(true)
                .message("Goal updated successfully")
                .data(goalService.update(id, request))
                .build());
    }
}
