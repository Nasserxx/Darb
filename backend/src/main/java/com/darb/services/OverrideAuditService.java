package com.darb.services;

import com.darb.entities.OverrideAuditLog;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.OverrideAuditLogRepository;
import com.darb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OverrideAuditService {

    public static final int MIN_REASON_LENGTH = 8;

    private final OverrideAuditLogRepository overrideAuditLogRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;

    public String requireAuditReason(String headerReason, String bodyReason) {
        String reason = firstNonBlank(trim(headerReason), trim(bodyReason));
        if (reason == null || reason.length() < MIN_REASON_LENGTH) {
            throw new BadRequestException(
                    "Audit reason required (min " + MIN_REASON_LENGTH + " characters) for super admin override");
        }
        return reason;
    }

    public String gateSuperAdmin(UUID callerId, String headerReason, String bodyReason) {
        UserRole role = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId))
                .getRole();
        if (role != UserRole.SUPER_ADMIN) {
            return null;
        }
        return requireAuditReason(headerReason, bodyReason);
    }

    @Transactional
    public void record(UUID actorId, UUID mosqueId, String action, String reason,
                       String resourceType, UUID resourceId) {
        OverrideAuditLog entry = OverrideAuditLog.builder()
                .actor(userRepository.getReferenceById(actorId))
                .mosque(mosqueId != null ? mosqueRepository.getReferenceById(mosqueId) : null)
                .action(action)
                .reason(reason)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();
        overrideAuditLogRepository.save(entry);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null) {
            return first;
        }
        return second;
    }
}
