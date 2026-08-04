package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.parentstudent.ParentStudentCreateRequest;
import com.darb.dtos.parentstudent.ParentStudentJoinPreviewResponse;
import com.darb.dtos.parentstudent.ParentStudentJoinRequest;
import com.darb.dtos.parentstudent.ParentStudentResponse;
import com.darb.dtos.parentstudent.ParentStudentUpdateRequest;
import com.darb.dtos.student.StudentResponse;
import com.darb.services.ParentStudentService;

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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parent-students")
@RequiredArgsConstructor
@Tag(name = "Parent-Student Relationship Management", description = "Endpoints for managing parent-child relationships, guardianship, and notification preferences")
public class ParentStudentController {

    private final ParentStudentService parentStudentService;

    @GetMapping
    @Operation(
            summary = "List all parent-student relationships",
            description = "Returns a paginated list of all parent-student relationships. Parents can only see their own children. Admins see all."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Parent-student relationships retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'PARENT')")
    public ResponseEntity<ApiResponse<PageResponse<ParentStudentResponse>>> findAll(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ParentStudentResponse> page = parentStudentService.findAll((UUID) authentication.getPrincipal(), pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<ParentStudentResponse>>builder()
                .success(true)
                .message("Parent-student relationships retrieved successfully")
                .data(PageResponse.<ParentStudentResponse>builder()
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
            summary = "Get parent-student relationship by ID",
            description = "Retrieves a specific parent-student relationship by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationship retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relationship not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN', 'PARENT')")
    public ResponseEntity<ApiResponse<ParentStudentResponse>> findById(
            Authentication authentication,
            @Parameter(description = "Parent-student relationship UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<ParentStudentResponse>builder()
                .success(true)
                .message("Parent-student relationship retrieved successfully")
                .data(parentStudentService.findById((UUID) authentication.getPrincipal(), id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Create parent-student relationship",
            description = "Links a parent user to a student with specified relationship details. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Relationship created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<ParentStudentResponse>> create(
            Authentication authentication,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReasonHeader,
            @Valid @RequestBody ParentStudentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ParentStudentResponse>builder()
                        .success(true)
                        .message("Parent-student relationship created successfully")
                        .data(parentStudentService.create(
                                (UUID) authentication.getPrincipal(),
                                request,
                                auditReasonHeader,
                                request.getAuditReason()))
                        .build());
    }

    @GetMapping("/join/preview")
    @Operation(
            summary = "Preview parent-student link by invite code",
            description = "Returns the mosque and student name for a valid parent invite code."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preview retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invalid invite code")
    })
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ParentStudentJoinPreviewResponse>> previewJoin(
            @Parameter(description = "Parent invite code", required = true) @RequestParam("code") String code) {
        return ResponseEntity.ok(ApiResponse.<ParentStudentJoinPreviewResponse>builder()
                .success(true)
                .message("Preview retrieved successfully")
                .data(parentStudentService.previewJoinByInviteCode(code))
                .build());
    }

    @PostMapping("/join")
    @Operation(
            summary = "Link parent to student via invite code",
            description = "Allows a PARENT user to link themselves to a student using a parent invite code provided by the mosque admin."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Linked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or already linked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invalid invite code")
    })
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ParentStudentResponse>> join(
            Authentication authentication,
            @Valid @RequestBody ParentStudentJoinRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ParentStudentResponse>builder()
                        .success(true)
                        .message("Parent-student relationship created successfully")
                        .data(parentStudentService.joinByInviteCode(userId, request.getInviteCode()))
                        .build());
    }

    @GetMapping("/my-children")
    @Operation(
            summary = "Get my children",
            description = "Returns the list of students linked to the authenticated parent user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Children retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> getMyChildren(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<StudentResponse> children = parentStudentService.getMyChildren(userId);
        return ResponseEntity.ok(ApiResponse.<List<StudentResponse>>builder()
                .success(true)
                .message("Children retrieved successfully")
                .data(children)
                .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update parent-student relationship",
            description = "Updates an existing parent-student relationship's details such as notification preferences. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationship updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relationship not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<ParentStudentResponse>> update(
            Authentication authentication,
            @Parameter(description = "Parent-student relationship UUID", required = true) @PathVariable UUID id,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReasonHeader,
            @Valid @RequestBody ParentStudentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<ParentStudentResponse>builder()
                .success(true)
                .message("Parent-student relationship updated successfully")
                .data(parentStudentService.update(
                        (UUID) authentication.getPrincipal(),
                        id,
                        request,
                        auditReasonHeader,
                        request.getAuditReason()))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remove parent-student relationship",
            description = "Removes the link between a parent and student. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relationship removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Relationship not found")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication authentication,
            @Parameter(description = "Parent-student relationship UUID", required = true) @PathVariable UUID id,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReasonHeader) {
        parentStudentService.delete(
                (UUID) authentication.getPrincipal(),
                id,
                auditReasonHeader,
                null);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Parent-student relationship removed successfully")
                .build());
    }
}
