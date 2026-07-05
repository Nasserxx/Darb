package com.darb.services;

import com.darb.dtos.mosque.MosqueCreateRequest;
import com.darb.dtos.mosque.MosqueInviteCodesResponse;
import com.darb.dtos.mosque.MosqueJoinPreviewResponse;
import com.darb.dtos.mosque.MosqueOnboardResponse;
import com.darb.dtos.mosque.MosqueResponse;
import com.darb.dtos.mosque.MosqueSearchResult;
import com.darb.dtos.mosque.MosqueUpdateRequest;
import com.darb.dtos.mosqueadmin.MosqueAdminResponse;
import com.darb.entities.Mosque;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.User;
import com.darb.entities.enums.AdminPermission;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.darb.entities.enums.JoinRequestStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MosqueService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder INVITE_CODE_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int INVITE_CODE_BYTE_LENGTH = 9;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MosqueRepository mosqueRepository;
    private final MosqueAdminRepository mosqueAdminRepository;
    private final MosqueMemberJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final MosqueAccessService mosqueAccessService;

    @Transactional(readOnly = true)
    public Page<MosqueResponse> findAll(Pageable pageable) {
        return mosqueRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MosqueResponse findById(UUID callerId, UUID id) {
        mosqueAccessService.assertCanAccessMosque(callerId, id);
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MosqueJoinPreviewResponse previewMemberJoin(String inviteCode, UserRole role) {
        String normalizedCode = normalizeInviteCode(inviteCode);
        Mosque mosque = findMosqueByMemberInviteCode(normalizedCode, role)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "inviteCode", normalizedCode));

        return MosqueJoinPreviewResponse.builder()
                .mosqueName(mosque.getName())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MosqueSearchResult> searchMosques(UUID userId, String q, String city) {
        User user = findUserOrThrow(userId);
        if (user.getRole() != UserRole.TEACHER && user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Only teachers and students can search mosques");
        }
        if (joinRequestRepository.existsByUserIdAndStatus(userId, JoinRequestStatus.PENDING)) {
            throw new ForbiddenException("You already have a pending join request");
        }

        String query = q == null ? "" : q.trim();
        String cityFilter = city == null ? "" : city.trim();
        return mosqueRepository.searchActive(query, cityFilter).stream()
                .map(mosque -> MosqueSearchResult.builder()
                        .id(mosque.getId())
                        .name(mosque.getName())
                        .city(mosque.getCity())
                        .build())
                .toList();
    }

    @Transactional
    public MosqueInviteCodesResponse getInviteCodes(UUID adminUserId) {
        UUID mosqueId = mosqueAccessService.requireMosqueIdForAdmin(adminUserId);
        Mosque mosque = findEntityOrThrow(mosqueId);
        ObjectNode settings = readSettingsObject(mosque.getSettings());
        ensureMemberInviteCodes(settings);
        mosque.setSettings(writeSettingsObject(settings));
        mosqueRepository.save(mosque);

        return MosqueInviteCodesResponse.builder()
                .adminInviteCode(textOrNull(settings, "adminInviteCode"))
                .teacherInviteCode(textOrNull(settings, "teacherInviteCode"))
                .studentInviteCode(textOrNull(settings, "studentInviteCode"))
                .build();
    }

    @Transactional(readOnly = true)
    public MosqueJoinPreviewResponse previewJoin(UUID userId, String inviteCode) {
        User user = findUserOrThrow(userId);
        assertEligibleForSelfServiceOnboarding(user);
        return previewJoin(inviteCode);
    }

    @Transactional(readOnly = true)
    public MosqueJoinPreviewResponse previewJoin(String inviteCode) {
        String normalizedCode = normalizeInviteCode(inviteCode);
        Mosque mosque = mosqueRepository.findActiveByAdminInviteCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "inviteCode", normalizedCode));

        return MosqueJoinPreviewResponse.builder()
                .mosqueName(mosque.getName())
                .build();
    }

    @Transactional
    public MosqueResponse create(MosqueCreateRequest request) {
        Mosque mosque = Mosque.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .timezone(request.getTimezone())
                .isActive(true)
                .build();

        return toResponse(mosqueRepository.save(mosque));
    }

    @Transactional
    public MosqueOnboardResponse onboardMosque(UUID userId, MosqueCreateRequest request) {
        User user = findUserOrThrow(userId);
        assertEligibleForSelfServiceOnboarding(user);

        String adminInviteCode = generateInviteCode();
        String teacherInviteCode = generateInviteCode();
        String studentInviteCode = generateInviteCode();
        Mosque mosque = Mosque.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .timezone(request.getTimezone())
                .settings(buildSettingsWithInviteCodes(adminInviteCode, teacherInviteCode, studentInviteCode))
                .isActive(true)
                .build();
        mosque = mosqueRepository.save(mosque);

        MosqueAdmin mosqueAdmin = createMosqueAdmin(user, mosque, user, AdminPermission.FULL_ACCESS, true);
        mosqueAdmin = mosqueAdminRepository.save(mosqueAdmin);

        return MosqueOnboardResponse.builder()
                .mosque(toResponse(mosque))
                .admin(toAdminResponse(mosqueAdmin))
                .inviteCode(adminInviteCode)
                .teacherInviteCode(teacherInviteCode)
                .studentInviteCode(studentInviteCode)
                .build();
    }

    @Transactional
    public MosqueOnboardResponse joinMosque(UUID userId, String inviteCode) {
        User user = findUserOrThrow(userId);
        assertEligibleForSelfServiceOnboarding(user);

        String normalizedCode = normalizeInviteCode(inviteCode);
        Mosque mosque = mosqueRepository.findActiveByAdminInviteCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "inviteCode", normalizedCode));

        if (mosqueAdminRepository.existsByUserIdAndMosqueId(userId, mosque.getId())) {
            throw new ForbiddenException("You are already assigned to this mosque");
        }

        MosqueAdmin mosqueAdmin = createMosqueAdmin(user, mosque, user, AdminPermission.FULL_ACCESS, false);
        mosqueAdmin = mosqueAdminRepository.save(mosqueAdmin);

        return MosqueOnboardResponse.builder()
                .mosque(toResponse(mosque))
                .admin(toAdminResponse(mosqueAdmin))
                .build();
    }

    @Transactional
    public MosqueResponse update(UUID callerId, UUID id, MosqueUpdateRequest request) {
        mosqueAccessService.assertCanAccessMosque(callerId, id);
        Mosque mosque = findEntityOrThrow(id);

        if (request.getName() != null) {
            mosque.setName(request.getName());
        }
        if (request.getAddress() != null) {
            mosque.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            mosque.setCity(request.getCity());
        }
        if (request.getPhone() != null) {
            mosque.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            mosque.setEmail(request.getEmail());
        }
        if (request.getLogoUrl() != null) {
            mosque.setLogoUrl(request.getLogoUrl());
        }
        if (request.getTimezone() != null) {
            mosque.setTimezone(request.getTimezone());
        }
        if (request.getSettings() != null) {
            mosque.setSettings(mergeSettingsPreservingInviteCode(mosque.getSettings(), request.getSettings()));
        }

        return toResponse(mosqueRepository.save(mosque));
    }

    @Transactional
    public void delete(UUID id) {
        Mosque mosque = findEntityOrThrow(id);
        mosque.setIsActive(false);
        mosqueRepository.save(mosque);
    }

    private void assertEligibleForSelfServiceOnboarding(User user) {
        if (user.getRole() != UserRole.MOSQUE_ADMIN) {
            throw new ForbiddenException("Only mosque administrators can use this endpoint");
        }
        if (mosqueAdminRepository.existsByUserId(user.getId())) {
            throw new ForbiddenException("You already have a mosque assignment");
        }
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private MosqueAdmin createMosqueAdmin(
            User user,
            Mosque mosque,
            User assignedBy,
            AdminPermission permission,
            boolean isPrimaryAdmin) {
        return MosqueAdmin.builder()
                .user(user)
                .mosque(mosque)
                .permission(permission)
                .isPrimaryAdmin(isPrimaryAdmin)
                .assignedAt(Instant.now())
                .assignedBy(assignedBy)
                .build();
    }

    private String generateInviteCode() {
        byte[] bytes = new byte[INVITE_CODE_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return INVITE_CODE_ENCODER.encodeToString(bytes);
    }

    private String buildSettingsWithInviteCodes(
            String adminInviteCode,
            String teacherInviteCode,
            String studentInviteCode) {
        try {
            ObjectNode node = OBJECT_MAPPER.createObjectNode();
            node.put("adminInviteCode", adminInviteCode);
            node.put("teacherInviteCode", teacherInviteCode);
            node.put("studentInviteCode", studentInviteCode);
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to build mosque settings");
        }
    }

    private java.util.Optional<Mosque> findMosqueByMemberInviteCode(String inviteCode, UserRole role) {
        return switch (role) {
            case TEACHER -> mosqueRepository.findActiveByTeacherInviteCode(inviteCode);
            case STUDENT -> mosqueRepository.findActiveByStudentInviteCode(inviteCode);
            default -> java.util.Optional.empty();
        };
    }

    private ObjectNode readSettingsObject(String settings) {
        if (settings == null || settings.isBlank()) {
            return OBJECT_MAPPER.createObjectNode();
        }
        try {
            return (ObjectNode) OBJECT_MAPPER.readTree(settings);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse mosque settings", e);
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    private String writeSettingsObject(ObjectNode node) {
        try {
            return node.isEmpty() ? null : OBJECT_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to serialize mosque settings");
        }
    }

    private void ensureMemberInviteCodes(ObjectNode settings) {
        if (!settings.hasNonNull("adminInviteCode")) {
            settings.put("adminInviteCode", generateInviteCode());
        }
        if (!settings.hasNonNull("teacherInviteCode")) {
            settings.put("teacherInviteCode", generateInviteCode());
        }
        if (!settings.hasNonNull("studentInviteCode")) {
            settings.put("studentInviteCode", generateInviteCode());
        }
    }

    private String textOrNull(ObjectNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new BadRequestException("Invite code is required");
        }
        return inviteCode.trim();
    }

    private Mosque findEntityOrThrow(UUID id) {
        return mosqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", id));
    }

    private MosqueResponse toResponse(Mosque mosque) {
        return MosqueResponse.builder()
                .id(mosque.getId())
                .name(mosque.getName())
                .address(mosque.getAddress())
                .city(mosque.getCity())
                .phone(mosque.getPhone())
                .email(mosque.getEmail())
                .logoUrl(mosque.getLogoUrl())
                .timezone(mosque.getTimezone())
                .settings(stripInviteCodeFromSettings(mosque.getSettings()))
                .isActive(mosque.getIsActive())
                .createdAt(mosque.getCreatedAt())
                .build();
    }

    private String stripInviteCodeFromSettings(String settings) {
        if (settings == null || settings.isBlank()) {
            return settings;
        }
        try {
            ObjectNode node = (ObjectNode) OBJECT_MAPPER.readTree(settings);
            node.remove("adminInviteCode");
            node.remove("teacherInviteCode");
            node.remove("studentInviteCode");
            return node.isEmpty() ? null : OBJECT_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.warn("Failed to strip adminInviteCode from settings", e);
            return settings;
        }
    }

    private String mergeSettingsPreservingInviteCode(String existingSettings, String newSettings) {
        try {
            ObjectNode merged = (ObjectNode) OBJECT_MAPPER.readTree(newSettings);
            if (existingSettings != null && !existingSettings.isBlank()) {
                ObjectNode existing = (ObjectNode) OBJECT_MAPPER.readTree(existingSettings);
                for (String key : List.of("adminInviteCode", "teacherInviteCode", "studentInviteCode")) {
                    if (!merged.has(key) && existing.has(key)) {
                        merged.set(key, existing.get(key));
                    }
                }
            }
            return OBJECT_MAPPER.writeValueAsString(merged);
        } catch (JsonProcessingException e) {
            log.warn("Failed to merge settings; using request settings as-is", e);
            return newSettings;
        }
    }

    private MosqueAdminResponse toAdminResponse(MosqueAdmin mosqueAdmin) {
        return MosqueAdminResponse.builder()
                .id(mosqueAdmin.getId())
                .userId(mosqueAdmin.getUser().getId())
                .mosqueId(mosqueAdmin.getMosque().getId())
                .permission(mosqueAdmin.getPermission())
                .isPrimaryAdmin(mosqueAdmin.getIsPrimaryAdmin())
                .assignedAt(mosqueAdmin.getAssignedAt())
                .assignedBy(mosqueAdmin.getAssignedBy().getId())
                .build();
    }
}
