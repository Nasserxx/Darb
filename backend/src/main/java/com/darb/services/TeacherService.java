package com.darb.services;

import com.darb.dtos.mosque.MemberJoinRequestResponse;
import com.darb.dtos.teacher.TeacherCreateRequest;
import com.darb.dtos.teacher.TeacherProvisionRequest;
import com.darb.dtos.teacher.TeacherResponse;
import com.darb.dtos.teacher.TeacherUpdateRequest;
import com.darb.dtos.user.UserCreateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.darb.util.NameFilterSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;
    private final MosqueMemberJoinRequestRepository joinRequestRepository;
    private final MosqueAccessService mosqueAccessService;
    private final UserService userService;
    private final ObjectProvider<MosqueMemberJoinRequestService> joinRequestService;

    @Transactional(readOnly = true)
    public Page<TeacherResponse> findAll(UUID callerId, Pageable pageable, UUID mosqueIdFilter, String q) {
        mosqueAccessService.assertValidNameFilter(callerId, mosqueIdFilter, q);
        if (mosqueAccessService.shouldReturnEmptyListPage(callerId)) {
            return Page.empty(pageable);
        }
        UUID mosqueId = mosqueAccessService.resolveEffectiveMosqueIdForList(callerId, mosqueIdFilter);
        Specification<Teacher> spec = (root, query, cb) -> cb.conjunction();
        if (mosqueId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mosque").get("id"), mosqueId));
        }
        spec = NameFilterSpecs.and(spec, NameFilterSpecs.userJoinFullNameLike("user", q));
        return teacherRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(UUID callerId, UUID id) {
        Teacher teacher = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessTeacher(callerId, teacher);
        return toResponse(teacher);
    }

    @Transactional
    public MemberJoinRequestResponse create(UUID callerId, TeacherCreateRequest request) {
        UUID mosqueId = mosqueAccessService.resolveMosqueIdForAdminMutation(callerId, request.getMosqueId());
        return joinRequestService.getObject().invite(
                callerId, request.getUserId(), mosqueId, UserRole.TEACHER, null, null);
    }

    @Transactional
    public TeacherResponse provision(UUID callerId, TeacherProvisionRequest request) {
        UUID mosqueId = mosqueAccessService.resolveMosqueIdForAdminMutation(callerId, request.getMosqueId());
        UserCreateRequest userRequest = new UserCreateRequest();
        userRequest.setFullName(request.getFullName());
        userRequest.setEmail(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setPhone(request.getPhone());
        userRequest.setRole(UserRole.TEACHER);
        userRequest.setGender(request.getGender());
        userRequest.setDateOfBirth(request.getDateOfBirth());
        userRequest.setCity(request.getCity());
        userRequest.setAddressCountry(request.getAddressCountry());
        userRequest.setAddressPostalCode(request.getAddressPostalCode());
        userRequest.setAddressStreet(request.getAddressStreet());
        userRequest.setAddressHouseNumber(request.getAddressHouseNumber());
        userRequest.setAddressState(request.getAddressState());
        UUID userId = userService.create(userRequest).getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));
        TeacherResponse response = attachSeat(
                user, mosque, request.getSpecialization(), request.getBio(),
                request.getYearsExperience(), request.getIjazahChain());
        log.info("Provisioned user {} mosque {} role TEACHER", userId, mosque.getId());
        return response;
    }

    @Transactional
    public TeacherResponse joinByInviteCode(UUID userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.TEACHER) {
            throw new ForbiddenException("Only teachers can use this endpoint");
        }

        String normalizedCode = inviteCode == null ? "" : inviteCode.trim();
        if (normalizedCode.isBlank()) {
            throw new com.darb.exceptions.BadRequestException("Invite code is required");
        }

        Mosque mosque = mosqueRepository.findActiveByTeacherInviteCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "inviteCode", normalizedCode));

        return onboard(userId, mosque.getId());
    }

    @Transactional
    public TeacherResponse onboard(UUID userId, UUID mosqueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.TEACHER) {
            throw new ForbiddenException("Only teachers can use this endpoint");
        }
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));
        if (teacherRepository.existsByUserIdAndMosqueIdAndIsActiveTrue(userId, mosqueId)) {
            throw new ForbiddenException("You already have a teacher profile at this mosque");
        }
        if (joinRequestRepository.existsByUserIdAndMosqueIdAndRequestedRoleAndStatus(
                userId, mosqueId, UserRole.TEACHER, JoinRequestStatus.PENDING)) {
            throw new ForbiddenException("You already have a pending join request");
        }
        return attachSeat(user, mosque, null, null, null, null);
    }

    public TeacherResponse completeApprovedJoin(UUID userId, UUID mosqueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));
        if (teacherRepository.existsByUserIdAndMosqueIdAndIsActiveTrue(userId, mosqueId)) {
            throw new ForbiddenException("You already have a teacher profile at this mosque");
        }
        return attachSeat(user, mosque, null, null, null, null);
    }

    private TeacherResponse attachSeat(
            User user,
            Mosque mosque,
            String specialization,
            String bio,
            Integer yearsExperience,
            String ijazahChain) {
        Teacher existing = teacherRepository.findByUserId(user.getId()).stream()
                .filter(t -> t.getMosque().getId().equals(mosque.getId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setSpecialization(specialization);
            existing.setBio(bio);
            existing.setYearsExperience(yearsExperience);
            existing.setIjazahChain(ijazahChain);
            existing.setIsAvailable(true);
            existing.setIsActive(true);
            existing.setJoinedAt(Instant.now());
            return toResponse(teacherRepository.save(existing));
        }
        Teacher teacher = Teacher.builder()
                .user(user)
                .mosque(mosque)
                .specialization(specialization)
                .bio(bio)
                .yearsExperience(yearsExperience)
                .ijazahChain(ijazahChain)
                .isAvailable(true)
                .isActive(true)
                .joinedAt(Instant.now())
                .build();
        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(UUID callerId, UUID id, TeacherUpdateRequest request) {
        Teacher teacher = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessTeacher(callerId, teacher);

        if (request.getSpecialization() != null) {
            teacher.setSpecialization(request.getSpecialization());
        }
        if (request.getBio() != null) {
            teacher.setBio(request.getBio());
        }
        if (request.getYearsExperience() != null) {
            teacher.setYearsExperience(request.getYearsExperience());
        }
        if (request.getIjazahChain() != null) {
            teacher.setIjazahChain(request.getIjazahChain());
        }
        if (request.getIsAvailable() != null) {
            teacher.setIsAvailable(request.getIsAvailable());
        }

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public void delete(UUID callerId, UUID id) {
        Teacher teacher = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessTeacher(callerId, teacher);
        teacher.setIsActive(false);
        teacherRepository.save(teacher);
    }

    private Teacher findEntityOrThrow(UUID id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
    }

    private TeacherResponse toResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUser().getId())
                .userName(teacher.getUser().getFullName())
                .mosqueId(teacher.getMosque().getId())
                .specialization(teacher.getSpecialization())
                .bio(teacher.getBio())
                .yearsExperience(teacher.getYearsExperience())
                .ijazahChain(teacher.getIjazahChain())
                .isAvailable(teacher.getIsAvailable())
                .isActive(teacher.getIsActive())
                .joinedAt(teacher.getJoinedAt())
                .build();
    }
}
