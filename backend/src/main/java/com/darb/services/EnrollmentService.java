package com.darb.services;

import com.darb.dtos.enrollment.EnrollmentCreateRequest;
import com.darb.dtos.enrollment.EnrollmentResponse;
import com.darb.dtos.enrollment.EnrollmentUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
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

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findAll(Pageable pageable) {
        return enrollmentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public EnrollmentResponse create(EnrollmentCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
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
    public EnrollmentResponse update(UUID id, EnrollmentUpdateRequest request) {
        Enrollment enrollment = findEntityOrThrow(id);

        if (request.getStatus() != null) {
            enrollment.setStatus(request.getStatus());
        }
        if (request.getWithdrawnDate() != null) {
            enrollment.setWithdrawnDate(request.getWithdrawnDate());
        }
        if (request.getNotes() != null) {
            enrollment.setNotes(request.getNotes());
        }

        return toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public void delete(UUID id) {
        Enrollment enrollment = findEntityOrThrow(id);
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
