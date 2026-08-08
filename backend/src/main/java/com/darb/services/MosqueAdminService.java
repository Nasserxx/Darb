package com.darb.services;

import com.darb.dtos.mosqueadmin.MosqueAdminCreateRequest;
import com.darb.dtos.mosqueadmin.MosqueAdminResponse;
import com.darb.dtos.mosqueadmin.MosqueAdminUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
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
public class MosqueAdminService {

    private final MosqueAdminRepository mosqueAdminRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;
    private final MosqueAccessService mosqueAccessService;
    private final OverrideAuditService overrideAuditService;

    @Transactional(readOnly = true)
    public Page<MosqueAdminResponse> findAll(UUID callerId, Pageable pageable) {
        return mosqueAccessService.pageForCaller(
                callerId,
                pageable,
                mosqueId -> mosqueAdminRepository.findByMosqueId(mosqueId, pageable),
                mosqueAdminRepository::findAll
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MosqueAdminResponse findById(UUID callerId, UUID id) {
        MosqueAdmin mosqueAdmin = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessMosque(callerId, mosqueAdmin.getMosque().getId());
        return toResponse(mosqueAdmin);
    }

    @Transactional
    public MosqueAdminResponse create(UUID callerId, MosqueAdminCreateRequest request,
                                      String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        User assignedBy = userRepository.findById(request.getAssignedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedBy()));

        MosqueAdmin mosqueAdmin = MosqueAdmin.builder()
                .user(user)
                .mosque(mosque)
                .permission(request.getPermission())
                .isPrimaryAdmin(request.getIsPrimaryAdmin() != null ? request.getIsPrimaryAdmin() : false)
                .assignedAt(Instant.now())
                .assignedBy(assignedBy)
                .build();

        MosqueAdmin saved = mosqueAdminRepository.save(mosqueAdmin);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    mosque.getId(),
                    "MOSQUE_ADMIN_ASSIGN",
                    auditReason,
                    "MosqueAdmin",
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public MosqueAdminResponse update(UUID callerId, UUID id, MosqueAdminUpdateRequest request,
                                      String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        MosqueAdmin mosqueAdmin = findEntityOrThrow(id);

        if (request.getPermission() != null) {
            mosqueAdmin.setPermission(request.getPermission());
        }
        if (request.getIsPrimaryAdmin() != null) {
            mosqueAdmin.setIsPrimaryAdmin(request.getIsPrimaryAdmin());
        }

        MosqueAdmin saved = mosqueAdminRepository.save(mosqueAdmin);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    mosqueAdmin.getMosque().getId(),
                    "MOSQUE_ADMIN_UPDATE",
                    auditReason,
                    "MosqueAdmin",
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID callerId, UUID id, String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        MosqueAdmin mosqueAdmin = findEntityOrThrow(id);
        UUID mosqueId = mosqueAdmin.getMosque().getId();
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    mosqueId,
                    "MOSQUE_ADMIN_REMOVE",
                    auditReason,
                    "MosqueAdmin",
                    id);
        }
        mosqueAdminRepository.deleteById(id);
    }

    private MosqueAdmin findEntityOrThrow(UUID id) {
        return mosqueAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MosqueAdmin", "id", id));
    }

    private MosqueAdminResponse toResponse(MosqueAdmin mosqueAdmin) {
        return MosqueAdminResponse.builder()
                .id(mosqueAdmin.getId())
                .userId(mosqueAdmin.getUser().getId())
                .userName(mosqueAdmin.getUser().getFullName())
                .mosqueId(mosqueAdmin.getMosque().getId())
                .mosqueName(mosqueAdmin.getMosque().getName())
                .permission(mosqueAdmin.getPermission())
                .isPrimaryAdmin(mosqueAdmin.getIsPrimaryAdmin())
                .assignedAt(mosqueAdmin.getAssignedAt())
                .assignedBy(mosqueAdmin.getAssignedBy().getId())
                .build();
    }
}
