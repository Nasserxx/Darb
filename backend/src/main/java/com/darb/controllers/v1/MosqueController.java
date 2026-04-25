package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.mosque.MosqueCreateRequest;
import com.darb.dtos.mosque.MosqueResponse;
import com.darb.dtos.mosque.MosqueUpdateRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mosques")
@RequiredArgsConstructor
@Tag(name = "Mosque Management", description = "Endpoints for managing mosques in the Darb platform")
public class MosqueController {

    private final MosqueService mosqueService;

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
            @Parameter(description = "Mosque UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<MosqueResponse>builder()
                .success(true)
                .message("Mosque retrieved successfully")
                .data(mosqueService.findById(id))
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
            @Parameter(description = "Mosque UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody MosqueUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<MosqueResponse>builder()
                .success(true)
                .message("Mosque updated successfully")
                .data(mosqueService.update(id, request))
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
