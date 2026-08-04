package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.mosque.MosqueCreateRequest;
import com.darb.dtos.mosque.MosqueInviteCodesResponse;
import com.darb.dtos.mosque.MosqueJoinPreviewResponse;
import com.darb.dtos.mosque.MosqueJoinRequest;
import com.darb.dtos.mosque.MosqueOnboardResponse;
import com.darb.dtos.mosque.MosqueResponse;
import com.darb.dtos.mosque.MosqueSearchResult;
import com.darb.dtos.mosque.MosqueUpdateRequest;
import com.darb.dtos.mosque.MemberJoinRequestCreateRequest;
import com.darb.dtos.mosque.MemberJoinRequestResponse;
import com.darb.entities.enums.UserRole;
import com.darb.services.MosqueMemberJoinRequestService;
import com.darb.services.MosqueService;

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
@RequestMapping("/api/v1/mosques")
@RequiredArgsConstructor
@Tag(name = "Mosque Management", description = "Endpoints for managing mosques in the Darb platform")
public class MosqueController {

    private final MosqueService mosqueService;
    private final MosqueMemberJoinRequestService joinRequestService;

    @GetMapping
    @Operation(
            summary = "List all mosques",
            description = "Returns a paginated list of all registered mosques. Accessible to any authenticated user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosques retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<MosqueResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MosqueResponse> page = mosqueService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<MosqueResponse>>builder()
                .success(true)
                .message("Mosques retrieved successfully")
                .data(PageResponse.<MosqueResponse>builder()
                        .content(page.getContent())
                        .pageNumber(page.getNumber())
                        .pageSize(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build())
                .build());
    }

    @GetMapping("/member-join/preview")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<MosqueJoinPreviewResponse>> previewMemberJoin(
            @RequestParam("code") String code,
            @RequestParam("role") UserRole role) {
        return ResponseEntity.ok(ApiResponse.<MosqueJoinPreviewResponse>builder()
                .success(true)
                .message("Mosque preview retrieved successfully")
                .data(mosqueService.previewMemberJoin(code, role))
                .build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<MosqueSearchResult>>> search(
            Authentication authentication,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "city", required = false) String city) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<List<MosqueSearchResult>>builder()
                .success(true)
                .message("Mosques retrieved successfully")
                .data(mosqueService.searchMosques(userId, q, city))
                .build());
    }

    @GetMapping("/invite-codes")
    @PreAuthorize("hasRole('MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueInviteCodesResponse>> inviteCodes(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<MosqueInviteCodesResponse>builder()
                .success(true)
                .message("Invite codes retrieved successfully")
                .data(mosqueService.getInviteCodes(userId))
                .build());
    }

    @PostMapping("/join-requests")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<MemberJoinRequestResponse>> createJoinRequest(
            Authentication authentication,
            @Valid @RequestBody MemberJoinRequestCreateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MemberJoinRequestResponse>builder()
                        .success(true)
                        .message("Join request submitted successfully")
                        .data(joinRequestService.create(userId, request))
                        .build());
    }

    @DeleteMapping("/join-requests/my")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> cancelMyJoinRequest(
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        joinRequestService.cancelMyJoinRequest(userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Join request cancelled")
                .build());
    }

    @GetMapping("/join/preview")
    @Operation(
            summary = "Preview mosque by invite code",
            description = "Returns the mosque name for a valid admin invite code. "
                    + "Accessible by MOSQUE_ADMIN users who do not yet have a mosque assignment."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preview retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invalid invite code")
    })
    @PreAuthorize("hasRole('MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueJoinPreviewResponse>> previewJoin(
            Authentication authentication,
            @Parameter(description = "Admin invite code", required = true) @RequestParam("code") String code) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<MosqueJoinPreviewResponse>builder()
                .success(true)
                .message("Mosque preview retrieved successfully")
                .data(mosqueService.previewJoin(userId, code))
                .build());
    }

    @PostMapping("/onboard")
    @Operation(
            summary = "Create mosque and become primary admin",
            description = "Self-service onboarding for MOSQUE_ADMIN users without an existing mosque assignment. "
                    + "Creates the mosque, assigns the caller as primary admin, and returns an invite code."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Mosque onboarded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User already has a mosque assignment or wrong role")
    })
    @PreAuthorize("hasRole('MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueOnboardResponse>> onboard(
            Authentication authentication,
            @Valid @RequestBody MosqueCreateRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MosqueOnboardResponse>builder()
                        .success(true)
                        .message("Mosque onboarded successfully")
                        .data(mosqueService.onboardMosque(userId, request))
                        .build());
    }

    @PostMapping("/join")
    @Operation(
            summary = "Join mosque via invite code",
            description = "Self-service onboarding for MOSQUE_ADMIN users without an existing mosque assignment. "
                    + "Joins an existing mosque using a valid admin invite code."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Joined mosque successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User already has a mosque assignment or wrong role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invalid invite code")
    })
    @PreAuthorize("hasRole('MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueOnboardResponse>> join(
            Authentication authentication,
            @Valid @RequestBody MosqueJoinRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MosqueOnboardResponse>builder()
                        .success(true)
                        .message("Joined mosque successfully")
                        .data(mosqueService.joinMosque(userId, request.getInviteCode()))
                        .build());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get mosque by ID",
            description = "Retrieves a specific mosque's details by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MosqueResponse>> findById(
            Authentication authentication,
            @Parameter(description = "Mosque UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<MosqueResponse>builder()
                .success(true)
                .message("Mosque retrieved successfully")
                .data(mosqueService.findById((UUID) authentication.getPrincipal(), id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Create a new mosque",
            description = "Registers a new mosque in the system. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Mosque created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueResponse>> create(
            @Valid @RequestBody MosqueCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MosqueResponse>builder()
                        .success(true)
                        .message("Mosque created successfully")
                        .data(mosqueService.create(request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a mosque",
            description = "Updates an existing mosque's details. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<MosqueResponse>> update(
            Authentication authentication,
            @Parameter(description = "Mosque UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody MosqueUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<MosqueResponse>builder()
                .success(true)
                .message("Mosque updated successfully")
                .data(mosqueService.update((UUID) authentication.getPrincipal(), id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deactivate a mosque",
            description = "Soft-deletes a mosque by deactivating it. Mosque data is preserved but becomes inactive. Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque not found")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Mosque UUID", required = true) @PathVariable UUID id) {
        mosqueService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Mosque deactivated successfully")
                .build());
    }
}
