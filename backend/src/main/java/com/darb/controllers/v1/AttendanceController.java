package com.darb.controllers.v1;

import com.darb.dtos.attendance.AttendanceCreateRequest;
import com.darb.dtos.attendance.AttendanceExcuseRequest;
import com.darb.dtos.attendance.AttendanceResponse;
import com.darb.dtos.attendance.AttendanceUpdateRequest;
import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.services.AttendanceService;

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
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance Management", description = "Endpoints for recording and tracking student attendance at circle sessions")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    @Operation(
            summary = "List all attendance records",
            description = "Returns a paginated list of all attendance records. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance records retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> findAll(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceResponse> page = attendanceService.findAll((UUID) authentication.getPrincipal(), pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AttendanceResponse>>builder()
                .success(true)
                .message("Attendance records retrieved successfully")
                .data(PageResponse.<AttendanceResponse>builder()
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
            summary = "List attendance by circle",
            description = "Returns a paginated list of attendance records for a specific study circle. Accessible to any authenticated user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circle attendance retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Circle not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> findByCircle(
            Authentication authentication,
            @Parameter(description = "Circle UUID", required = true) @PathVariable UUID circleId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceResponse> page = attendanceService.findByCircleId((UUID) authentication.getPrincipal(), circleId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AttendanceResponse>>builder()
                .success(true)
                .message("Circle attendance retrieved successfully")
                .data(PageResponse.<AttendanceResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/student/{studentId}")
    @Operation(
            summary = "List attendance by student",
            description = "Returns a paginated list of attendance records for a specific student. Accessible by the student themselves, their parents, teachers, and admins."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student attendance retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied to this student")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> findByStudent(
            Authentication authentication,
            @Parameter(description = "Student UUID", required = true) @PathVariable UUID studentId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceResponse> page = attendanceService.findByStudentId((UUID) authentication.getPrincipal(), studentId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AttendanceResponse>>builder()
                .success(true)
                .message("Student attendance retrieved successfully")
                .data(PageResponse.<AttendanceResponse>builder()
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
            summary = "Get attendance record by ID",
            description = "Retrieves a specific attendance record by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance record retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attendance record not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceResponse>> findById(
            Authentication authentication,
            @Parameter(description = "Attendance record UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<AttendanceResponse>builder()
                .success(true)
                .message("Attendance record retrieved successfully")
                .data(attendanceService.findById((UUID) authentication.getPrincipal(), id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Record attendance",
            description = "Creates a new attendance record for a student in a circle session. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Attendance recorded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> create(
            Authentication authentication,
            @Valid @RequestBody AttendanceCreateRequest request) {
        UUID callerId = (UUID) authentication.getPrincipal();
        request.setRecordedBy(callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AttendanceResponse>builder()
                        .success(true)
                        .message("Attendance recorded successfully")
                        .data(attendanceService.create(callerId, request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update attendance record",
            description = "Updates an existing attendance record's status, check-in time, or absence reason. Accessible by SUPER_ADMIN, MOSQUE_ADMIN, and TEACHER."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attendance updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attendance record not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> update(
            Authentication authentication,
            @Parameter(description = "Attendance record UUID", required = true) @PathVariable UUID id,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReasonHeader,
            @Valid @RequestBody AttendanceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<AttendanceResponse>builder()
                .success(true)
                .message("Attendance updated successfully")
                .data(attendanceService.update(
                        (UUID) authentication.getPrincipal(),
                        id,
                        request,
                        auditReasonHeader,
                        request.getAuditReason()))
                .build());
    }

    @PostMapping("/{id}/excuse")
    @PreAuthorize("hasAnyRole('STUDENT', 'PARENT')")
    @Operation(summary = "Submit absence excuse", description = "Submit an excuse for an existing attendance record.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Excuse submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attendance record not found")
    })
    public ResponseEntity<ApiResponse<AttendanceResponse>> submitExcuse(
            @Parameter(description = "Attendance record UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody AttendanceExcuseRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        AttendanceResponse response = attendanceService.submitExcuse(id, userId, request);
        return ResponseEntity.ok(ApiResponse.<AttendanceResponse>builder()
                .success(true)
                .message("Excuse submitted")
                .data(response)
                .build());
    }
}
