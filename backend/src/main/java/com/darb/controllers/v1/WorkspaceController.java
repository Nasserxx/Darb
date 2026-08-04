package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.workspace.WorkspaceProfileResponse;
import com.darb.services.WorkspaceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "Authenticated user's workspace context")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/profile")
    @Operation(
            summary = "Get workspace profile",
            description = "Returns the role-specific workspace profile for the authenticated user."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WorkspaceProfileResponse>> getProfile(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<WorkspaceProfileResponse>builder()
                .success(true)
                .message("Workspace profile retrieved")
                .data(workspaceService.getProfile(userId))
                .build());
    }
}
