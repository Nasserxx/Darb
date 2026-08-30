package com.darb.services;

import com.darb.dtos.memorization.LessonAssignmentResponse;
import com.darb.dtos.memorization.LessonAssignmentUpsertRequest;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.LessonAssignment;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.LessonAssignmentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonAssignmentService {

    private final LessonAssignmentRepository lessonAssignmentRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final MosqueAccessService mosqueAccessService;
    private final MushafMapService mushafMapService;

    @Transactional(readOnly = true)
    public LessonAssignmentResponse get(UUID callerId, UUID studentId, UUID circleId) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        requireActiveEnrollment(studentId, circleId);
        LessonAssignment assignment = lessonAssignmentRepository.findByStudentIdAndCircleId(studentId, circleId)
                .orElseThrow(() -> new ResourceNotFoundException("LessonAssignment", "studentId/circleId",
                        studentId + "/" + circleId));
        return toResponse(assignment);
    }

    @Transactional
    public LessonAssignmentResponse upsert(UUID callerId, UUID studentId, LessonAssignmentUpsertRequest request) {
        assertStaffCanMutate(callerId, studentId);
        requireActiveEnrollment(studentId, request.getCircleId());
        mushafMapService.requireHalfPageIds(request.getHalfPageIds());

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        User assigner = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId));

        LessonAssignment assignment = lessonAssignmentRepository
                .findByStudentIdAndCircleId(studentId, request.getCircleId())
                .orElse(LessonAssignment.builder()
                        .student(student)
                        .circle(circle)
                        .build());

        assignment.setHalfPageIds(request.getHalfPageIds());
        assignment.setNote(request.getNote());
        assignment.setAssignedBy(assigner);
        assignment.setAssignedAt(Instant.now());

        return toResponse(lessonAssignmentRepository.save(assignment));
    }

    private void requireActiveEnrollment(UUID studentId, UUID circleId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCircleId(studentId, circleId)
                .orElseThrow(() -> new BadRequestException("memorization.noEnrollment"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("memorization.noEnrollment");
        }
    }

    private void assertStaffCanMutate(UUID callerId, UUID studentId) {
        UserRole role = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId))
                .getRole();
        if (role == UserRole.STUDENT) {
            throw new ForbiddenException("memorization.assessment.staffOnly");
        }
        mosqueAccessService.assertCanMutateStudent(callerId, studentId);
    }

    private LessonAssignmentResponse toResponse(LessonAssignment assignment) {
        return LessonAssignmentResponse.builder()
                .id(assignment.getId())
                .studentId(assignment.getStudent().getId())
                .circleId(assignment.getCircle().getId())
                .halfPageIds(assignment.getHalfPageIds())
                .note(assignment.getNote())
                .assignedBy(assignment.getAssignedBy().getId())
                .assignedAt(assignment.getAssignedAt())
                .build();
    }
}
