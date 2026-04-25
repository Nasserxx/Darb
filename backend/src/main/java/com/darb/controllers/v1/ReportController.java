package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.report.ReportCreateRequest;
import com.darb.dtos.report.ReportResponse;
import com.darb.services.ReportService;

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
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report Management", description = "Endpoints for generating, managing, and accessing analytical reports for mosques")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Get report by ID",
            description = "Retrieves a specific report by its unique identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Report retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Report not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> findById(
            @Parameter(description = "Report UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .success(true)
                .message("Report retrieved successfully")
                .data(reportService.findById(id))
                .build());
    }

    @GetMapping("/mosque/{mosqueId}")
    @Operation(
            summary = "List reports by mosque",
            description = "Returns a paginated list of reports generated for a specific mosque. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mosque reports retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mosque not found")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> findByMosque(
            @Parameter(description = "Mosque UUID", required = true) @PathVariable UUID mosqueId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReportResponse> page = reportService.findByMosqueId(mosqueId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<ReportResponse>>builder()
                .success(true)
                .message("Mosque reports retrieved successfully")
                .data(PageResponse.<ReportResponse>builder()
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
            summary = "Generate a report",
            description = "Creates a new analytical report for a mosque with specified filters and type. Accessible by SUPER_ADMIN and MOSQUE_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Report generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MOSQUE_ADMIN')")
    public ResponseEntity<ApiResponse<ReportResponse>> create(
            Authentication authentication,
            @Valid @RequestBody ReportCreateRequest request) {
        request.setGeneratedBy((UUID) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ReportResponse>builder()
                        .success(true)
                        .message("Report generated successfully")
                        .data(reportService.create(request))
                        .build());
    }
}
