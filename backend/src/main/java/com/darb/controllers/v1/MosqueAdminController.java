package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.mosqueadmin.MosqueAdminCreateRequest;
import com.darb.dtos.mosqueadmin.MosqueAdminResponse;
import com.darb.dtos.mosqueadmin.MosqueAdminUpdateRequest;
import com.darb.dtos.mosque.MemberJoinRequestResponse;
import com.darb.services.MosqueAdminService;
import com.darb.services.MosqueMemberJoinRequestService;

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
@RequestMapping("/api/v1/mosque-admins")
@RequiredArgsConstructor
@Tag(name = "Mosque Admin Management", description = "Endpoints for managing mosque administrator assignments and permissions")
public class MosqueAdminController {

    private final MosqueAdminService mosqueAdminService;
    private final MosqueMemberJoinRequestService joinRequestService;

    @GetMapping("/join-requests")
    @PreAuthorize("hasRole('MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberJoinRequestResponse>>> listJoinRequests(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<List<MemberJoinRequestResponse>>builder()
                .success(true)
                .message("Join requests retrieved successfully")
                .data(joinRequestService.listPendingForAdmin(userId))
                .build());
    }

    @PostMapping("/join-requests/{id}/approve")
    @PreAuthorize("hasRole('MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MemberJoinRequestResponse>> approveJoinRequest(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<MemberJoinRequestResponse>builder()
                .success(true)
                .message("Join request approved")
                .data(joinRequestService.approve(userId, id))
                .build());
    }

    @PostMapping("/join-requests/{id}/reject")
    @PreAuthorize("hasRole('MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MemberJoinRequestResponse>> rejectJoinRequest(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<MemberJoinRequestResponse>builder()
                .success(true)
                .message("Join request rejected")
                .data(joinRequestService.reject(userId, id))
                .build());
    }

    @GetMapping
    @Operation(
            summary = "List all mosque admin assignments",
            description = "Returns a paginated list of all mosque admin assignments. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque admins retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<MosqueAdminResponse>>> findAll(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MosqueAdminResponse> page = mosqueAdminService.findAll((UUID) authentication.getPrincipal(), pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<MosqueAdminResponse>>builder()
                .success(true)
                .message("Mosque admins retrieved successfully")
                .data(PageResponse.<MosqueAdminResponse>builder()
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
            summary = "Get mosque admin by ID",
            description = "Retrieves a specific mosque admin assignment by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque admin retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque admin assignment not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueAdminResponse>> findById(
            Authentication authentication,
            @Parameter(description = "Mosque admin assignment UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<MosqueAdminResponse>builder()
                .success(true)
                .message("Mosque admin retrieved successfully")
                .data(mosqueAdminService.findById((UUID) authentication.getPrincipal(), id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Assign mosque admin",
            description = "Assigns a user as an administrator for a mosque with specified permissions. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Mosque admin assigned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueAdminResponse>> create(
            Authentication authentication,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReasonHeader,
            @Valid @RequestBody MosqueAdminCreateRequest request) {
        request.setAssignedBy((UUID) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MosqueAdminResponse>builder()
                        .success(true)
                        .message("Mosque admin assigned successfully")
                        .data(mosqueAdminService.create(
                                (UUID) authentication.getPrincipal(),
                                request,
                                auditReasonHeader,
                                request.getAuditReason()))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update mosque admin assignment",
            description = "Updates the permissions or primary status of an existing mosque admin assignment. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque admin updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque admin assignment not found")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueAdminResponse>> update(
            Authentication authentication,
            @Parameter(description = "Mosque admin assignment UUID", required = true) @PathVariable UUID id,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReasonHeader,
            @Valid @RequestBody MosqueAdminUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<MosqueAdminResponse>builder()
                .success(true)
                .message("Mosque admin updated successfully")
                .data(mosqueAdminService.update(
                        (UUID) authentication.getPrincipal(),
                        id,
                        request,
                        auditReasonHeader,
                        request.getAuditReason()))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remove mosque admin assignment",
            description = "Removes a mosque admin assignment, revoking the user's administrative access to the mosque. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque admin removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque admin assignment not found")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication authentication,
            @Parameter(description = "Mosque admin assignment UUID", required = true) @PathVariable UUID id,
            @RequestHeader(value = "X-Audit-Reason", required = false) String auditReasonHeader) {
        mosqueAdminService.delete(
                (UUID) authentication.getPrincipal(),
                id,
                auditReasonHeader,
                null);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Mosque admin removed successfully")
                .build());
    }
}
