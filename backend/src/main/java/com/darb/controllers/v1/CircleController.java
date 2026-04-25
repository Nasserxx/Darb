package com.darb.controllers.v1;

import com.darb.dtos.circle.CircleCreateRequest;
import com.darb.dtos.circle.CircleResponse;
import com.darb.dtos.circle.CircleUpdateRequest;
import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.services.CircleService;

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
@RequestMapping("/api/v1/circles")
@RequiredArgsConstructor
@Tag(name = "Circle Management", description = "Endpoints for managing study circles (halaqat), scheduling, and capacity")
public class CircleController {

    private final CircleService circleService;

    @GetMapping
    @Operation(
            summary = "List all study circles",
            description = "Returns a paginated list of all study circles. Accessible to any authenticated user."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circles retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<CircleResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CircleResponse> page = circleService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<CircleResponse>>builder()
                .success(true)
                .message("Circles retrieved successfully")
                .data(PageResponse.<CircleResponse>builder()
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
            summary = "Get circle by ID",
            description = "Retrieves a specific study circle's details by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circle retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Circle not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CircleResponse>> findById(
            @Parameter(description = "Circle UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<CircleResponse>builder()
                .success(true)
                .message("Circle retrieved successfully")
                .data(circleService.findById(id))
                .build());
    }

    @PostMapping
    @Operation(
            summary = "Create a study circle",
            description = "Creates a new study circle with a teacher, mosque, schedule, and capacity. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Circle created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<CircleResponse>> create(
            @Valid @RequestBody CircleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CircleResponse>builder()
                        .success(true)
                        .message("Circle created successfully")
                        .data(circleService.create(request))
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a study circle",
            description = "Updates an existing study circle's schedule, capacity, or status. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circle updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Circle not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<CircleResponse>> update(
            @Parameter(description = "Circle UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody CircleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.<CircleResponse>builder()
                .success(true)
                .message("Circle updated successfully")
                .data(circleService.update(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deactivate a study circle",
            description = "Soft-deletes a study circle by deactivating it. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circle deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Circle not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Circle UUID", required = true) @PathVariable UUID id) {
        circleService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Circle deactivated successfully")
                .build());
    }
}
