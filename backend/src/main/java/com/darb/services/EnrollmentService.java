package com.darb.services;

import com.darb.dtos.enrollment.EnrollmentCreateRequest;
import com.darb.dtos.enrollment.EnrollmentResponse;
import com.darb.dtos.enrollment.EnrollmentUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.CircleStatus;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.StudentRepository;
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
    public Page<EnrollmentResponse> findAll(UUID callerId, Pageable pageable, UUID mosqueIdFilter, String q,
                                            EnrollmentStatus status) {
        mosqueAccessService.assertValidNameFilter(callerId, mosqueIdFilter, q);
        if (mosqueAccessService.shouldReturnEmptyListPage(callerId)) {
            return Page.empty(pageable);
        }
        UUID mosqueId = mosqueAccessService.resolveEffectiveMosqueIdForList(callerId, mosqueIdFilter);
        Specification<Enrollment> spec = (root, query, cb) -> cb.conjunction();
        if (mosqueId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("circle").get("mosque").get("id"), mosqueId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        spec = NameFilterSpecs.and(spec, NameFilterSpecs.enrollmentStudentFullNameLike(q));
        return enrollmentRepository.findAll(spec, pageable).map(this::toResponse);
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
    public EnrollmentResponse create(UUID callerId, EnrollmentCreateRequest request,
                                     String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));

        EnrollmentStatus status = request.getStatus() != null ? request.getStatus() : EnrollmentStatus.PENDING;
        boolean activating = status == EnrollmentStatus.ACTIVE;
        Circle circle = activating
                ? circleRepository.findByIdForUpdate(request.getCircleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()))
                : circleRepository.findById(request.getCircleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));

        mosqueAccessService.assertCanAccessStudent(callerId, student);
        mosqueAccessService.assertCanAccessMosque(callerId, circle.getMosque().getId());

        if (!student.getMosque().getId().equals(circle.getMosque().getId())) {
            throw new BadRequestException("Student and circle must belong to the same mosque");
        }

        if (activating) {
            assertCanActivateInCircle(circle);
        }

        User approvedBy = userRepository.findById(request.getApprovedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getApprovedBy()));

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .circle(circle)
                .status(status)
                .enrolledDate(request.getEnrolledDate() != null ? request.getEnrolledDate() : LocalDate.now())
                .approvedBy(approvedBy)
                .notes(request.getNotes())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    circle.getMosque().getId(),
                    "ENROLLMENT_CREATE",
                    auditReason,
                    "Enrollment",
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public EnrollmentResponse update(UUID callerId, UUID id, EnrollmentUpdateRequest request,
                                     String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        Enrollment enrollment = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, enrollment.getStudent().getId());

        if (request.getStatus() != null) {
            if (request.getStatus() == EnrollmentStatus.ACTIVE) {
                if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                    // D17: already ACTIVE → idempotent 200
                    return toResponse(enrollment);
                }
                Circle circle = circleRepository.findByIdForUpdate(enrollment.getCircle().getId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Circle", "id", enrollment.getCircle().getId()));
                assertCanActivateInCircle(circle);
            }
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

    /** D19 + D2: circle must be ACTIVE; ACTIVE enrollments must fit capacity (null = unlimited). */
    private void assertCanActivateInCircle(Circle circle) {
        if (circle.getStatus() != CircleStatus.ACTIVE) {
            throw new BadRequestException("Cannot activate enrollment: circle is not ACTIVE");
        }
        Integer capacity = circle.getCapacity();
        if (capacity == null) {
            return;
        }
        long activeCount = enrollmentRepository.countByCircleIdAndStatus(
                circle.getId(), EnrollmentStatus.ACTIVE);
        if (activeCount >= capacity) {
            throw new BadRequestException("Circle is at capacity");
        }
    }

    private Enrollment findEntityOrThrow(UUID id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", id));
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getUser().getFullName())
                .circleId(enrollment.getCircle().getId())
                .circleName(enrollment.getCircle().getName())
                .status(enrollment.getStatus())
                .enrolledDate(enrollment.getEnrolledDate())
                .withdrawnDate(enrollment.getWithdrawnDate())
                .approvedBy(enrollment.getApprovedBy().getId())
                .notes(enrollment.getNotes())
                .build();
    }
}
