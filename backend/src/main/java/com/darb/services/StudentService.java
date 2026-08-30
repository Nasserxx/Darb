package com.darb.services;

import com.darb.dtos.mosque.MemberJoinRequestResponse;
import com.darb.dtos.student.StudentCreateRequest;
import com.darb.dtos.student.StudentProvisionRequest;
import com.darb.dtos.student.StudentResponse;
import com.darb.dtos.student.StudentUpdateRequest;
import com.darb.dtos.user.UserCreateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.StudentRepository;
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

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder INVITE_CODE_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int INVITE_CODE_BYTE_LENGTH = 9;

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;
    private final MosqueMemberJoinRequestRepository joinRequestRepository;
    private final MosqueAccessService mosqueAccessService;
    private final UserService userService;
    private final ObjectProvider<MosqueMemberJoinRequestService> joinRequestService;

    @Transactional(readOnly = true)
    public Page<StudentResponse> findAll(UUID callerId, Pageable pageable, UUID mosqueIdFilter, String q) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId));
        if (caller.getRole() == UserRole.STUDENT) {
            List<Student> mine = studentRepository.findByUserId(callerId);
            return new org.springframework.data.domain.PageImpl<>(
                    mine.stream().map(this::toResponse).toList(),
                    pageable,
                    mine.size());
        }

        mosqueAccessService.assertValidNameFilter(callerId, mosqueIdFilter, q);
        if (mosqueAccessService.shouldReturnEmptyListPage(callerId)) {
            return Page.empty(pageable);
        }
        UUID mosqueId = mosqueAccessService.resolveEffectiveMosqueIdForList(callerId, mosqueIdFilter);
        Specification<Student> spec = (root, query, cb) -> cb.conjunction();
        if (mosqueId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mosque").get("id"), mosqueId));
        }
        spec = NameFilterSpecs.and(spec, NameFilterSpecs.userJoinFullNameLike("user", q));
        return studentRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(UUID callerId, UUID id) {
        Student student = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, student);
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public Student findByUserId(UUID userId) {
        return studentRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
    }

    @Transactional
    public MemberJoinRequestResponse create(UUID callerId, StudentCreateRequest request) {
        UUID mosqueId = mosqueAccessService.resolveMosqueIdForAdminMutation(callerId, request.getMosqueId());
        return joinRequestService.getObject().invite(
                callerId, request.getUserId(), mosqueId, UserRole.STUDENT, null, null);
    }

    @Transactional
    public StudentResponse provision(UUID callerId, StudentProvisionRequest request) {
        UUID mosqueId = mosqueAccessService.resolveMosqueIdForAdminMutation(callerId, request.getMosqueId());
        UserCreateRequest userRequest = new UserCreateRequest();
        userRequest.setFullName(request.getFullName());
        userRequest.setEmail(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setPhone(request.getPhone());
        userRequest.setRole(UserRole.STUDENT);
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
        StudentResponse response = attachSeat(
                user, mosque, request.getMedicalNotes(), request.getMemorizedJuz(), generateParentInviteCode());
        log.info("Provisioned user {} mosque {} role STUDENT", userId, mosque.getId());
        return response;
    }

    @Transactional
    public StudentResponse joinByInviteCode(UUID userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Only students can use this endpoint");
        }

        String normalizedCode = inviteCode == null ? "" : inviteCode.trim();
        if (normalizedCode.isBlank()) {
            throw new com.darb.exceptions.BadRequestException("Invite code is required");
        }

        Mosque mosque = mosqueRepository.findActiveByStudentInviteCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "inviteCode", normalizedCode));

        return onboard(userId, mosque.getId());
    }

    @Transactional
    public StudentResponse onboard(UUID userId, UUID mosqueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Only students can use this endpoint");
        }
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));
        if (studentRepository.existsByUserIdAndMosqueIdAndStatus(
                userId, mosqueId, EnrollmentStatus.ACTIVE)) {
            throw new ForbiddenException("You already have a student profile at this mosque");
        }
        if (joinRequestRepository.existsByUserIdAndMosqueIdAndRequestedRoleAndStatus(
                userId, mosqueId, UserRole.STUDENT, JoinRequestStatus.PENDING)) {
            throw new ForbiddenException("You already have a pending join request");
        }
        return attachSeat(user, mosque, null, null, generateParentInviteCode());
    }

    public StudentResponse completeApprovedJoin(UUID userId, UUID mosqueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));
        if (studentRepository.existsByUserIdAndMosqueIdAndStatus(
                userId, mosqueId, EnrollmentStatus.ACTIVE)) {
            throw new ForbiddenException("You already have a student profile at this mosque");
        }
        return attachSeat(user, mosque, null, null, generateParentInviteCode());
    }

    private StudentResponse attachSeat(
            User user, Mosque mosque, String medicalNotes, Integer memorizedJuz, String parentInviteCode) {
        Student existing = studentRepository.findByUserId(user.getId()).stream()
                .filter(s -> s.getMosque().getId().equals(mosque.getId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setMedicalNotes(medicalNotes);
            existing.setMemorizedJuz(memorizedJuz);
            if (parentInviteCode != null) {
                existing.setParentInviteCode(parentInviteCode);
            }
            existing.setStatus(EnrollmentStatus.ACTIVE);
            existing.setEnrolledAt(Instant.now());
            return toResponse(studentRepository.save(existing));
        }
        Student student = Student.builder()
                .user(user)
                .mosque(mosque)
                .medicalNotes(medicalNotes)
                .memorizedJuz(memorizedJuz)
                .parentInviteCode(parentInviteCode)
                .totalAbsences(0)
                .totalLateArrivals(0)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build();
        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse update(UUID callerId, UUID id, StudentUpdateRequest request) {
        Student student = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, student);

        if (request.getMedicalNotes() != null) {
            student.setMedicalNotes(request.getMedicalNotes());
        }
        if (request.getMemorizedJuz() != null) {
            student.setMemorizedJuz(request.getMemorizedJuz());
        }
        if (request.getParentInviteCode() != null) {
            student.setParentInviteCode(request.getParentInviteCode());
        }
        if (request.getFullName() != null) {
            student.getUser().setFullName(request.getFullName());
        }

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void delete(UUID callerId, UUID id) {
        Student student = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, student);
        student.setStatus(EnrollmentStatus.WITHDRAWN);
        studentRepository.save(student);
    }

    @Transactional
    public StudentResponse regenerateParentInviteCode(UUID callerId, UUID studentId) {
        Student student = findEntityOrThrow(studentId);
        mosqueAccessService.assertCanAccessStudent(callerId, student);
        student.setParentInviteCode(generateParentInviteCode());
        return toResponse(studentRepository.save(student));
    }

    private static String generateParentInviteCode() {
        byte[] bytes = new byte[INVITE_CODE_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return INVITE_CODE_ENCODER.encodeToString(bytes);
    }

    private Student findEntityOrThrow(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    private StudentResponse toResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .userId(student.getUser().getId())
                .fullName(student.getUser().getFullName())
                .mosqueId(student.getMosque().getId())
                .mosqueName(student.getMosque().getName())
                .medicalNotes(student.getMedicalNotes())
                .memorizedJuz(student.getMemorizedJuz())
                .totalAbsences(student.getTotalAbsences())
                .totalLateArrivals(student.getTotalLateArrivals())
                .status(student.getStatus())
                .enrolledAt(student.getEnrolledAt())
                .parentInviteCode(student.getParentInviteCode())
                .build();
    }
}
