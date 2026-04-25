package com.darb.controllers.v1;

import com.darb.dtos.common.ApiResponse;
import com.darb.dtos.common.PageResponse;
import com.darb.dtos.message.MessageCreateRequest;
import com.darb.dtos.message.MessageResponse;
import com.darb.services.MessageService;

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
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Message Management", description = "Endpoints for sending and managing direct and circle-threaded messages between users")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/mine")
    @Operation(
            summary = "List current user's messages",
            description = "Returns a paginated list of messages where the authenticated user is either sender or receiver."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> findMine(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = (UUID) authentication.getPrincipal();
        Page<MessageResponse> page = messageService.findByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<MessageResponse>>builder()
                .success(true)
                .message("Messages retrieved successfully")
                .data(PageResponse.<MessageResponse>builder()
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
            summary = "List messages by circle",
            description = "Returns a paginated list of messages within a specific circle thread."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Circle messages retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format or pagination parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Circle not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> findByCircle(
            @Parameter(description = "Circle UUID", required = true) @PathVariable UUID circleId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MessageResponse> page = messageService.findByCircleId(circleId, pageable);
        return ResponseEntity.ok(ApiResponse.<PageResponse<MessageResponse>>builder()
                .success(true)
                .message("Circle messages retrieved successfully")
                .data(PageResponse.<MessageResponse>builder()
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
            summary = "Send a message",
            description = "Sends a new message from the authenticated user to another user, optionally within a circle thread."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or validation errors"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponse>> create(
            Authentication authentication,
            @Valid @RequestBody MessageCreateRequest request) {
        request.setSenderId((UUID) authentication.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Message sent successfully")
                        .data(messageService.create(request))
                        .build());
    }

    @PutMapping("/{id}/read")
    @Operation(
            summary = "Mark message as read",
            description = "Marks a specific message as read by updating its status to READ."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Message not found")
    })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MessageResponse>> markAsRead(
            @Parameter(description = "Message UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<MessageResponse>builder()
                .success(true)
                .message("Message marked as read")
                .data(messageService.markAsRead(id))
                .build());
    }
}
