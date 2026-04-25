package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.student.StudentCreateRequest;
import com.darb.dtos.student.StudentResponse;
import com.darb.dtos.student.StudentUpdateRequest;
import com.darb.services.StudentService;

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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "Endpoints for managing student profiles, enrollment status, and academic records")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(
            summary = "List all students",
            description = "Returns a paginated list of all student profiles. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Students retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<StudentResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StudentResponse> page = studentService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<StudentResponse>>builder()
                .success(true)
                .message("Students retrieved successfully")
                .data(PageResponse.<StudentResponse>builder()
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
            summary = "Get student by ID",
            description = "Retrieves a specific student's profile by their unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StudentResponse>> findById(
            @Parameter(description = "Student UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<StudentResponse>builder()
                .success(true)
                .message("Student retrieved successfully")
                .data(studentService.findById(id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Create a student profile",
            description = "Creates a new student profile linked to an existing user and mosque. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Student created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> create(
            @Valid @RequestBody StudentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<StudentResponse>builder()
                        .success(true)
                        .message("Student created successfully")
                        .data(studentService.create(request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a student profile",
            description = "Updates an existing student's profile information. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<StudentResponse>> update(
            @Parameter(description = "Student UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody StudentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<StudentResponse>builder()
                .success(true)
                .message("Student updated successfully")
                .data(studentService.update(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deactivate a student profile",
            description = "Soft-deletes a student profile by deactivating it. Only accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Student UUID", required = true) @PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Student deactivated successfully")
                .build());
    }
}
