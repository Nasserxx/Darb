package com.darb.services;

import com.darb.dtos.mosqueadmin.MosqueAdminCreateRequest;
import com.darb.dtos.mosqueadmin.MosqueAdminResponse;
import com.darb.dtos.mosqueadmin.MosqueAdminUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.User;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.darb.util.NameFilterSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    public Page<MosqueAdminResponse> findAll(UUID callerId, Pageable pageable, UUID mosqueIdFilter, String q) {
        mosqueAccessService.assertValidNameFilter(callerId, mosqueIdFilter, q);
        if (mosqueAccessService.shouldReturnEmptyListPage(callerId)) {
            return Page.empty(pageable);
        }
        UUID mosqueId = mosqueAccessService.resolveEffectiveMosqueIdForList(callerId, mosqueIdFilter);
        Specification<MosqueAdmin> spec = (root, query, cb) -> cb.conjunction();
        if (mosqueId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mosque").get("id"), mosqueId));
        }
        spec = NameFilterSpecs.and(spec, NameFilterSpecs.userJoinFullNameLike("user", q));
        return mosqueAdminRepository.findAll(spec, pageable).map(this::toResponse);
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

        boolean isPrimary = request.getIsPrimaryAdmin() != null ? request.getIsPrimaryAdmin() : false;
        MosqueAdmin mosqueAdmin = mosqueAdminRepository
                .findByUserIdAndMosqueId(request.getUserId(), request.getMosqueId())
                .orElse(null);
        if (mosqueAdmin != null && Boolean.FALSE.equals(mosqueAdmin.getIsActive())) {
            mosqueAdmin.setPermission(request.getPermission());
            mosqueAdmin.setIsPrimaryAdmin(isPrimary);
            mosqueAdmin.setAssignedAt(Instant.now());
            mosqueAdmin.setAssignedBy(assignedBy);
            mosqueAdmin.setIsActive(true);
            mosqueAdmin.setLeftAt(null);
        } else {
            mosqueAdmin = MosqueAdmin.builder()
                    .user(user)
                    .mosque(mosque)
                    .permission(request.getPermission())
                    .isPrimaryAdmin(isPrimary)
                    .assignedAt(Instant.now())
                    .assignedBy(assignedBy)
                    .isActive(true)
                    .build();
        }

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
        MosqueAdmin mosqueAdmin = findEntityOrThrow(id);
        assertActive(mosqueAdmin);
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);

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
        MosqueAdmin mosqueAdmin = findEntityOrThrow(id);
        assertActive(mosqueAdmin);
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
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
        mosqueAdmin.setIsActive(false);
        mosqueAdmin.setLeftAt(Instant.now());
        mosqueAdminRepository.save(mosqueAdmin);
    }

    private MosqueAdmin findEntityOrThrow(UUID id) {
        return mosqueAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MosqueAdmin", "id", id));
    }

    private static void assertActive(MosqueAdmin mosqueAdmin) {
        if (Boolean.FALSE.equals(mosqueAdmin.getIsActive())) {
            throw new BadRequestException("Cannot mutate inactive mosque admin assignment");
        }
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
                .isActive(mosqueAdmin.getIsActive())
                .leftAt(mosqueAdmin.getLeftAt())
                .build();
    }
}
