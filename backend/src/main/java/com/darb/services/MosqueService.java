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
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MosqueService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder INVITE_CODE_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int INVITE_CODE_BYTE_LENGTH = 9;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_TIME_ZONE = "Asia/Riyadh";

    private final MosqueRepository mosqueRepository;
    private final MosqueAdminRepository mosqueAdminRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final MosqueAccessService mosqueAccessService;
    private final OverrideAuditService overrideAuditService;

    @Transactional(readOnly = true)
    public Page<MosqueResponse> findAll(UUID callerId, String q, String country, String city, Pageable pageable) {
        User user = findUserOrThrow(callerId);
        UserRole role = user.getRole();

        if (role == UserRole.SUPER_ADMIN) {
            Specification<Mosque> spec = buildFilterSpec(q, country, city);
            return mosqueRepository.findAll(spec, withStableSort(pageable)).map(this::toResponse);
        }

        if (role == UserRole.PARENT) {
            List<UUID> mosqueIds = parentStudentRepository.findByParentId(callerId).stream()
                    .map(link -> link.getStudent().getMosque().getId())
                    .distinct()
                    .toList();
            if (mosqueIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return mosqueRepository.findByIsActiveTrueAndIdIn(mosqueIds, pageable).map(this::toResponse);
        }

        UUID mosqueId = mosqueAccessService.resolveCallerMosqueId(callerId, role);
        if (mosqueId == null) {
            return Page.empty(pageable);
        }
        return mosqueRepository.findByIsActiveTrueAndIdIn(List.of(mosqueId), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<String> listCities(String country, boolean activeOnly) {
        if (!StringUtils.hasText(country)) {
            throw new BadRequestException("Invalid country code: " + country);
        }
        String normalized = country.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z]{2}$")) {
            throw new BadRequestException("Invalid country code: " + country);
        }
        return mosqueRepository.findDistinctCitiesByCountry(normalized, activeOnly);
    }

    private Specification<Mosque> buildFilterSpec(String q, String country, String city) {
        Specification<Mosque> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(q)) {
            String like = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), like, '\\'));
        }
        if (StringUtils.hasText(country)) {
            String normalized = country.trim().toUpperCase(Locale.ROOT);
            if (!normalized.matches("^[A-Z]{2}$")) {
                throw new BadRequestException("Invalid country code: " + country);
            }
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("addressCountry")), normalized));
        }
        if (StringUtils.hasText(city)) {
            String like = "%" + escapeLike(city.trim().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("city")), like, '\\'));
        }
        return spec;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Pageable withStableSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
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
                .addressCountry(mosque.getAddressCountry())
                .addressPostalCode(mosque.getAddressPostalCode())
                .addressStreet(mosque.getAddressStreet())
                .addressHouseNumber(mosque.getAddressHouseNumber())
                .addressState(mosque.getAddressState())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MosqueSearchResult> searchMosques(UUID userId, String q, String country, String city, Pageable pageable) {
        User user = findUserOrThrow(userId);
        if (user.getRole() != UserRole.TEACHER && user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Only teachers and students can search mosques");
        }
        // ponytail: pending is per mosque+role now; search stays open so users can find a second mosque

        Specification<Mosque> spec = buildFilterSpec(q, country, city)
                .and((root, query, cb) -> cb.isTrue(root.get("isActive")));
        return mosqueRepository.findAll(spec, withStableSort(pageable)).map(this::toSearchResult);
    }

    @Transactional
    public MosqueInviteCodesResponse getInviteCodes(UUID callerId, UUID mosqueId) {
        mosqueAccessService.requireMosqueAdminWith(callerId, mosqueId);
        Mosque mosque = findEntityOrThrow(mosqueId);
        ObjectNode settings = readSettingsObject(mosque.getSettings());
        ensureMemberInviteCodes(settings);
        mosque.setSettings(writeSettingsObject(settings));
        mosqueRepository.save(mosque);

        return toInviteCodesResponse(settings);
    }

    @Transactional
    public MosqueInviteCodesResponse rotateInviteCodes(UUID callerId, UUID mosqueId) {
        mosqueAccessService.requireMosqueAdminWith(callerId, mosqueId);
        Mosque mosque = findEntityOrThrow(mosqueId);
        return toInviteCodesResponse(regenerateMosqueInviteCodes(mosque));
    }

    private ObjectNode regenerateMosqueInviteCodes(Mosque mosque) {
        ObjectNode settings = readSettingsObject(mosque.getSettings());
        settings.put("adminInviteCode", generateInviteCode());
        settings.put("teacherInviteCode", generateInviteCode());
        settings.put("studentInviteCode", generateInviteCode());
        mosque.setSettings(writeSettingsObject(settings));
        mosqueRepository.save(mosque);
        return settings;
    }

    private MosqueInviteCodesResponse toInviteCodesResponse(ObjectNode settings) {
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
                .addressCountry(mosque.getAddressCountry())
                .addressPostalCode(mosque.getAddressPostalCode())
                .addressStreet(mosque.getAddressStreet())
                .addressHouseNumber(mosque.getAddressHouseNumber())
                .addressState(mosque.getAddressState())
                .build();
    }

    @Transactional
    public MosqueResponse create(MosqueCreateRequest request) {
        String adminInviteCode = generateInviteCode();
        String teacherInviteCode = generateInviteCode();
        String studentInviteCode = generateInviteCode();
        Mosque mosque = Mosque.builder()
                .name(request.getName())
                .city(request.getCity())
                .addressCountry(request.getAddressCountry())
                .addressPostalCode(request.getAddressPostalCode())
                .addressStreet(request.getAddressStreet())
                .addressHouseNumber(request.getAddressHouseNumber())
                .addressState(request.getAddressState())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .timezone(normalizeTimezone(request.getTimezone()))
                .settings(buildSettingsWithInviteCodes(adminInviteCode, teacherInviteCode, studentInviteCode))
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
                .city(request.getCity())
                .addressCountry(request.getAddressCountry())
                .addressPostalCode(request.getAddressPostalCode())
                .addressStreet(request.getAddressStreet())
                .addressHouseNumber(request.getAddressHouseNumber())
                .addressState(request.getAddressState())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .timezone(normalizeTimezone(request.getTimezone()))
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

        if (mosqueAdminRepository.existsByUserIdAndMosqueIdAndIsActiveTrue(userId, mosque.getId())) {
            throw new ForbiddenException("You are already assigned to this mosque");
        }

        MosqueAdmin mosqueAdmin = mosqueAdminRepository
                .findByUserIdAndMosqueId(userId, mosque.getId())
                .orElse(null);
        if (mosqueAdmin != null) {
            mosqueAdmin.setPermission(AdminPermission.MANAGE_TEACHERS);
            mosqueAdmin.setIsPrimaryAdmin(false);
            mosqueAdmin.setAssignedAt(Instant.now());
            mosqueAdmin.setAssignedBy(user);
            mosqueAdmin.setIsActive(true);
            mosqueAdmin.setLeftAt(null);
        } else {
            mosqueAdmin = createMosqueAdmin(user, mosque, user, AdminPermission.MANAGE_TEACHERS, false);
        }
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
        if (request.getCity() != null) {
            mosque.setCity(request.getCity());
        }
        if (request.getAddressCountry() != null) {
            mosque.setAddressCountry(request.getAddressCountry());
        }
        if (request.getAddressPostalCode() != null) {
            mosque.setAddressPostalCode(request.getAddressPostalCode());
        }
        if (request.getAddressStreet() != null) {
            mosque.setAddressStreet(request.getAddressStreet());
        }
        if (request.getAddressHouseNumber() != null) {
            mosque.setAddressHouseNumber(request.getAddressHouseNumber());
        }
        if (request.getAddressState() != null) {
            mosque.setAddressState(request.getAddressState());
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
            mosque.setTimezone(normalizeTimezone(request.getTimezone()));
        }
        if (request.getSettings() != null) {
            mosque.setSettings(mergeSettingsPreservingInviteCode(mosque.getSettings(), request.getSettings()));
        }

        return toResponse(mosqueRepository.save(mosque));
    }

    @Transactional
    public void delete(UUID callerId, UUID id, String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        Mosque mosque = findEntityOrThrow(id);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    mosque.getId(),
                    "MOSQUE_DEACTIVATE",
                    auditReason,
                    "Mosque",
                    mosque.getId());
        }
        mosque.setIsActive(false);
        mosqueRepository.save(mosque);
    }

    @Transactional
    public void reactivate(UUID id) {
        Mosque mosque = findEntityOrThrow(id);
        mosque.setIsActive(true);
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
                .isActive(true)
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

    private String normalizeTimezone(String timezone) {
        return (timezone == null || timezone.isBlank()) ? DEFAULT_TIME_ZONE : timezone;
    }

    private Mosque findEntityOrThrow(UUID id) {
        return mosqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", id));
    }

    private MosqueSearchResult toSearchResult(Mosque mosque) {
        return MosqueSearchResult.builder()
                .id(mosque.getId())
                .name(mosque.getName())
                .city(mosque.getCity())
                .addressCountry(mosque.getAddressCountry())
                .addressPostalCode(mosque.getAddressPostalCode())
                .addressStreet(mosque.getAddressStreet())
                .addressHouseNumber(mosque.getAddressHouseNumber())
                .addressState(mosque.getAddressState())
                .build();
    }

    private MosqueResponse toResponse(Mosque mosque) {
        return MosqueResponse.builder()
                .id(mosque.getId())
                .name(mosque.getName())
                .city(mosque.getCity())
                .addressCountry(mosque.getAddressCountry())
                .addressPostalCode(mosque.getAddressPostalCode())
                .addressStreet(mosque.getAddressStreet())
                .addressHouseNumber(mosque.getAddressHouseNumber())
                .addressState(mosque.getAddressState())
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
