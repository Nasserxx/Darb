package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.memorization.LessonAssignmentResponse;
import com.darb.dtos.memorization.LessonAssignmentUpsertRequest;
import com.darb.dtos.memorization.MemorizationAttemptCreateRequest;
import com.darb.dtos.memorization.MemorizationAttemptResponse;
import com.darb.dtos.memorization.MemorizationAttemptUpdateRequest;
import com.darb.dtos.memorization.MemorizationCoverageResponse;
import com.darb.dtos.mushaf.HalfPageEntry;
import com.darb.dtos.mushaf.MushafMetadataResponse;
import com.darb.entities.enums.PageHalf;
import com.darb.services.LessonAssignmentService;
import com.darb.services.MemorizationAttemptService;
import com.darb.services.MushafMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memorization")
@RequiredArgsConstructor
@Tag(name = "Memorization Mushaf", description = "Half-page mushaf memorization attempts, lessons, and coverage")
public class MemorizationMushafController {

    private final MushafMapService mushafMapService;
    private final MemorizationAttemptService memorizationAttemptService;
    private final LessonAssignmentService lessonAssignmentService;

    @GetMapping("/mushaf/madinah-604/metadata")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get full Madinah 604 mushaf metadata")
    public ResponseEntity<ApiResponse<MushafMetadataResponse>> getMushafMetadata(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<MushafMetadataResponse>builder()
                .success(true)
                .message("Mushaf metadata retrieved successfully")
                .data(mushafMapService.getMetadata())
                .build());
    }

    @GetMapping("/mushaf/madinah-604/metadata/juz/{juz}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get mushaf halves for one juz")
    public ResponseEntity<ApiResponse<List<HalfPageEntry>>> getJuzMetadata(
            Authentication authentication,
            @Parameter(description = "Juz number 1-30") @PathVariable int juz) {
        return ResponseEntity.ok(ApiResponse.<List<HalfPageEntry>>builder()
                .success(true)
                .message("Juz metadata retrieved successfully")
                .data(mushafMapService.getHalvesForJuz(juz))
                .build());
    }

    @GetMapping("/mushaf/madinah-604/pages/{page}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get mushaf metadata for one page")
    public ResponseEntity<ApiResponse<List<HalfPageEntry>>> getPageMetadata(
            Authentication authentication,
            @Parameter(description = "Page number 1-604") @PathVariable int page) {
        return ResponseEntity.ok(ApiResponse.<List<HalfPageEntry>>builder()
                .success(true)
                .message("Page metadata retrieved successfully")
                .data(mushafMapService.getHalvesForPage(page))
                .build());
    }

    @GetMapping("/students/{studentId}/coverage")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get memorization coverage for a student")
    public ResponseEntity<ApiResponse<MemorizationCoverageResponse>> getCoverage(
            Authentication authentication,
            @PathVariable UUID studentId,
            @RequestParam(required = true) UUID circleId,
            @RequestParam(defaultValue = "juz") String grain) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<MemorizationCoverageResponse>builder()
                .success(true)
                .message("Coverage retrieved successfully")
                .data(memorizationAttemptService.getCoverage(callerId, studentId, circleId, grain))
                .build());
    }

    @GetMapping("/students/{studentId}/attempts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List memorization attempts for a student")
    public ResponseEntity<ApiResponse<List<MemorizationAttemptResponse>>> listAttempts(
            Authentication authentication,
            @PathVariable UUID studentId,
            @RequestParam(required = true) UUID circleId,
            @RequestParam(required = false) Integer juz,
            @RequestParam(required = false) Short page) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<List<MemorizationAttemptResponse>>builder()
                .success(true)
                .message("Attempts retrieved successfully")
                .data(memorizationAttemptService.listAttempts(callerId, studentId, circleId, juz, page))
                .build());
    }

    @GetMapping("/students/{studentId}/attempts/{page}/{half}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get latest attempt for a half-page")
    public ResponseEntity<ApiResponse<MemorizationAttemptResponse>> getAttempt(
            Authentication authentication,
            @PathVariable UUID studentId,
            @PathVariable short page,
            @PathVariable PageHalf half,
            @RequestParam(required = true) UUID circleId) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<MemorizationAttemptResponse>builder()
                .success(true)
                .message("Attempt retrieved successfully")
                .data(memorizationAttemptService.getAttempt(callerId, studentId, page, half, circleId))
                .build());
    }

    @PostMapping("/students/{studentId}/attempts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create or upsert a half-page memorization attempt")
    public ResponseEntity<ApiResponse<MemorizationAttemptResponse>> upsertAttempt(
            Authentication authentication,
            @PathVariable UUID studentId,
            @Valid @RequestBody MemorizationAttemptCreateRequest request) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MemorizationAttemptResponse>builder()
                        .success(true)
                        .message("Attempt saved successfully")
                        .data(memorizationAttemptService.upsert(callerId, studentId, request))
                        .build());
    }

    @PutMapping("/attempts/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update attempt notes and grade")
    public ResponseEntity<ApiResponse<MemorizationAttemptResponse>> updateAttempt(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody MemorizationAttemptUpdateRequest request) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<MemorizationAttemptResponse>builder()
                .success(true)
                .message("Attempt updated successfully")
                .data(memorizationAttemptService.updateNotes(callerId, id, request))
                .build());
    }

    @GetMapping("/students/{studentId}/lesson")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current lesson assignment")
    public ResponseEntity<ApiResponse<LessonAssignmentResponse>> getLesson(
            Authentication authentication,
            @PathVariable UUID studentId,
            @RequestParam(required = true) UUID circleId) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<LessonAssignmentResponse>builder()
                .success(true)
                .message("Lesson retrieved successfully")
                .data(lessonAssignmentService.get(callerId, studentId, circleId))
                .build());
    }

    @PutMapping("/students/{studentId}/lesson")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Assign or replace current lesson")
    public ResponseEntity<ApiResponse<LessonAssignmentResponse>> upsertLesson(
            Authentication authentication,
            @PathVariable UUID studentId,
            @Valid @RequestBody LessonAssignmentUpsertRequest request) {
        UUID callerId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.<LessonAssignmentResponse>builder()
                .success(true)
                .message("Lesson saved successfully")
                .data(lessonAssignmentService.upsert(callerId, studentId, request))
                .build());
    }
}
