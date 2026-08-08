package com.darb.services;

import com.darb.dtos.student.StudentCreateRequest;
import com.darb.dtos.student.StudentResponse;
import com.darb.dtos.student.StudentUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.darb.entities.enums.JoinRequestStatus;
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
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;
    private final MosqueMemberJoinRequestRepository joinRequestRepository;
    private final MosqueAccessService mosqueAccessService;

    @Transactional(readOnly = true)
    public Page<StudentResponse> findAll(UUID callerId, Pageable pageable) {
        return mosqueAccessService.pageForCaller(
                callerId,
                pageable,
                mosqueId -> studentRepository.findByMosqueId(mosqueId, pageable),
                studentRepository::findAll
        ).map(this::toResponse);
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
    public StudentResponse create(UUID callerId, StudentCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        mosqueAccessService.assertCanAccessMosque(callerId, mosque.getId());

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        Student student = Student.builder()
                .user(user)
                .mosque(mosque)
                .nationalId(request.getNationalId())
                .medicalNotes(request.getMedicalNotes())
                .memorizedJuz(request.getMemorizedJuz())
                .parentInviteCode(request.getParentInviteCode())
                .totalAbsences(0)
                .totalLateArrivals(0)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build();

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse joinByInviteCode(UUID userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Only students can use this endpoint");
        }
        if (!studentRepository.findByUserId(userId).isEmpty()) {
            throw new ForbiddenException("You already have a student profile");
        }
        if (joinRequestRepository.existsByUserIdAndStatus(userId, JoinRequestStatus.PENDING)) {
            throw new ForbiddenException("You already have a pending join request");
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
        if (!studentRepository.findByUserId(userId).isEmpty()) {
            throw new ForbiddenException("You already have a student profile");
        }
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));

        Student student = Student.builder()
                .user(user)
                .mosque(mosque)
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

        if (request.getNationalId() != null) {
            student.setNationalId(request.getNationalId());
        }
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
                .nationalId(student.getNationalId())
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
