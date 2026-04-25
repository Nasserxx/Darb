package com.darb.services;

import com.darb.dtos.message.MessageCreateRequest;
import com.darb.dtos.message.MessageResponse;
import com.darb.dtos.message.MessageUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Message;
import com.darb.entities.User;
import com.darb.entities.enums.MessageStatus;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.MessageRepository;
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
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CircleRepository circleRepository;

    @Transactional(readOnly = true)
    public Page<MessageResponse> findAll(Pageable pageable) {
        return messageRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MessageResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public MessageResponse create(MessageCreateRequest request) {
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getSenderId()));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getReceiverId()));

        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .circle(circle)
                .content(request.getContent())
                .status(MessageStatus.SENT)
                .sentAt(Instant.now())
                .build();

        return toResponse(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> findByUserId(UUID userId, Pageable pageable) {
        return messageRepository.findBySenderIdOrReceiverId(userId, userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> findByCircleId(UUID circleId, Pageable pageable) {
        return messageRepository.findByCircleId(circleId, pageable).map(this::toResponse);
    }

    @Transactional
    public MessageResponse markAsRead(UUID id) {
        Message message = findEntityOrThrow(id);
        message.setStatus(MessageStatus.READ);
        message.setReadAt(Instant.now());
        return toResponse(messageRepository.save(message));
    }

    @Transactional
    public MessageResponse update(UUID id, MessageUpdateRequest request) {
        Message message = findEntityOrThrow(id);

        if (request.getStatus() != null) {
            message.setStatus(request.getStatus());
            if (request.getStatus() == MessageStatus.READ && message.getReadAt() == null) {
                message.setReadAt(Instant.now());
            }
        }

        return toResponse(messageRepository.save(message));
    }

    @Transactional
    public void delete(UUID id) {
        messageRepository.deleteById(id);
    }

    private Message findEntityOrThrow(UUID id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", id));
    }

    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .receiverId(message.getReceiver().getId())
                .circleId(message.getCircle().getId())
                .content(message.getContent())
                .status(message.getStatus())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .build();
    }
}
