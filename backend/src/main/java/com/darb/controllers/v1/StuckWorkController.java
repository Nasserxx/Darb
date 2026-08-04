package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.stuckwork.StuckWorkItem;
import com.darb.services.StuckWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stuck-work")
@RequiredArgsConstructor
@Tag(name = "Fleet Admin", description = "SUPER_ADMIN cross-mosque control plane")
public class StuckWorkController {

    private final StuckWorkService stuckWorkService;

    @GetMapping
    @Operation(
            summary = "List fleet stuck work",
            description = "Returns cross-mosque stuck work items (e.g. pending join requests). Only accessible by SUPER_ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stuck work retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<StuckWorkItem>>> listStuckWork() {
        return ResponseEntity.ok(ApiResponse.<List<StuckWorkItem>>builder()
                .success(true)
                .message("Stuck work retrieved successfully")
                .data(stuckWorkService.listFleetStuckWork())
                .build());
    }
}
