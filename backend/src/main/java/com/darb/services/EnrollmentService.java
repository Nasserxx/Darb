package com.darb.services;

import com.darb.dtos.enrollment.EnrollmentCreateRequest;
import com.darb.dtos.enrollment.EnrollmentResponse;
import com.darb.dtos.enrollment.EnrollmentUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final UserRepository userRepository;
    private final MosqueAccessService mosqueAccessService;
    private final OverrideAuditService overrideAuditService;

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findAll(UUID callerId, Pageable pageable) {
        return mosqueAccessService.pageForCaller(
                callerId, pageable,
                mosqueId -> enrollmentRepository.findByCircle_MosqueId(mosqueId, pageable),
                enrollmentRepository::findAll
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findByStudentId(UUID callerId, UUID studentId, Pageable pageable) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        return enrollmentRepository.findByStudentId(studentId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findById(UUID callerId, UUID id) {
        Enrollment enrollment = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, enrollment.getStudent().getId());
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse create(EnrollmentCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));

        if (!student.getMosque().getId().equals(circle.getMosque().getId())) {
            throw new BadRequestException("Student and circle must belong to the same mosque");
        }

        User approvedBy = userRepository.findById(request.getApprovedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getApprovedBy()));

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .circle(circle)
                .status(request.getStatus() != null ? request.getStatus() : EnrollmentStatus.PENDING)
                .enrolledDate(request.getEnrolledDate() != null ? request.getEnrolledDate() : LocalDate.now())
                .approvedBy(approvedBy)
                .notes(request.getNotes())
                .build();

        return toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public EnrollmentResponse update(UUID callerId, UUID id, EnrollmentUpdateRequest request,
                                     String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        Enrollment enrollment = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, enrollment.getStudent().getId());

        if (request.getStatus() != null) {
            enrollment.setStatus(request.getStatus());
        }
        if (request.getWithdrawnDate() != null) {
            enrollment.setWithdrawnDate(request.getWithdrawnDate());
        }
        if (request.getNotes() != null) {
            enrollment.setNotes(request.getNotes());
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        if (auditReason != null && request.getStatus() != null) {
            overrideAuditService.record(
                    callerId,
                    enrollment.getCircle().getMosque().getId(),
                    "ENROLLMENT_STATUS_FORCE",
                    auditReason,
                    "Enrollment",
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID callerId, UUID id) {
        Enrollment enrollment = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, enrollment.getStudent().getId());
        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollment.setWithdrawnDate(LocalDate.now());
        enrollmentRepository.save(enrollment);
    }

    private Enrollment findEntityOrThrow(UUID id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .circleId(enrollment.getCircle().getId())
                .status(enrollment.getStatus())
                .enrolledDate(enrollment.getEnrolledDate())
                .withdrawnDate(enrollment.getWithdrawnDate())
                .approvedBy(enrollment.getApprovedBy().getId())
                .notes(enrollment.getNotes())
                .build();
    }
}
