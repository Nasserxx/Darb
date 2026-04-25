package com.darb.services;

import com.darb.dtos.notification.NotificationCreateRequest;
import com.darb.dtos.notification.NotificationResponse;
import com.darb.dtos.notification.NotificationUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Notification;
import com.darb.entities.User;
import com.darb.entities.enums.NotificationStatus;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.NotificationRepository;
import com.darb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MosqueRepository mosqueRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findAll(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public NotificationResponse create(NotificationCreateRequest request) {
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        User recipient = userRepository.findById(request.getRecipientUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecipientUserId()));

        User sender = userRepository.findById(request.getSenderUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getSenderUserId()));

        Notification notification = Notification.builder()
                .mosque(mosque)
                .recipient(recipient)
                .sender(sender)
                .title(request.getTitle())
                .body(request.getBody())
                .channel(request.getChannel())
                .status(request.getStatus())
                .metadata(request.getMetadata())
                .build();

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findByRecipientId(UUID recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientId(recipientId, pageable).map(this::toResponse);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = findEntityOrThrow(id);
        notification.setStatus(NotificationStatus.READ);
        notification.setDeliveredAt(Instant.now());
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse update(UUID id, NotificationUpdateRequest request) {
        Notification notification = findEntityOrThrow(id);

        if (request.getStatus() != null) {
            notification.setStatus(request.getStatus());
        }
        if (request.getMetadata() != null) {
            notification.setMetadata(request.getMetadata());
        }

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void delete(UUID id) {
        notificationRepository.deleteById(id);
    }

    private Notification findEntityOrThrow(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .mosqueId(notification.getMosque().getId())
                .recipientUserId(notification.getRecipient().getId())
                .senderUserId(notification.getSender().getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .deliveredAt(notification.getDeliveredAt())
                .metadata(notification.getMetadata())
                .build();
    }
}
