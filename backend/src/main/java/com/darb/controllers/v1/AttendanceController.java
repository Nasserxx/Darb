package com.darb.controllers.v1;

import com.darb.dtos.attendance.AttendanceCreateRequest;
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
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceResponse> page = attendanceService.findAll(pageable);
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
            @Parameter(description = "Circle UUID", required = true) @PathVariable UUID circleId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceResponse> page = attendanceService.findByCircleId(circleId, pageable);
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
            @Parameter(description = "Attendance record UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<AttendanceResponse>builder()
                .success(true)
                .message("Attendance record retrieved successfully")
                .data(attendanceService.findById(id))
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
        request.setRecordedBy((UUID) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AttendanceResponse>builder()
                        .success(true)
                        .message("Attendance recorded successfully")
                        .data(attendanceService.create(request))
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
            @Parameter(description = "Attendance record UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody AttendanceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<AttendanceResponse>builder()
                .success(true)
                .message("Attendance updated successfully")
                .data(attendanceService.update(id, request))
                .build());
    }
}
