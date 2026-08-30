package com.darb.services;

import com.darb.dtos.memorization.MemorizationProgressCreateRequest;
import com.darb.dtos.memorization.MemorizationProgressResponse;
import com.darb.dtos.memorization.MemorizationProgressUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.MemorizationProgress;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.RecitationGrade;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.MemorizationProgressRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemorizationProgressService {

    private final MemorizationProgressRepository memorizationProgressRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final MosqueAccessService mosqueAccessService;

    @Transactional(readOnly = true)
    public Page<MemorizationProgressResponse> findAll(UUID callerId, Pageable pageable) {
        return mosqueAccessService.pageForCaller(
                callerId, pageable,
                mosqueId -> memorizationProgressRepository.findByCircle_MosqueId(mosqueId, pageable),
                memorizationProgressRepository::findAll
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MemorizationProgressResponse findById(UUID callerId, UUID id) {
        MemorizationProgress progress = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, progress.getStudent().getId());
        return toResponse(progress);
    }

    @Transactional(readOnly = true)
    public Page<MemorizationProgressResponse> findByStudentId(UUID callerId, UUID studentId, Pageable pageable) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        return memorizationProgressRepository.findByStudentId(studentId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MemorizationProgressResponse> findByCircleId(UUID callerId, UUID circleId, Pageable pageable) {
        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", circleId));
        UserRole role = findCallerRole(callerId);

        if (role == UserRole.PARENT) {
            Set<UUID> childIds = linkedChildIds(callerId);
            if (childIds.isEmpty()) {
                return Page.empty(pageable);
            }
            List<MemorizationProgress> filtered = memorizationProgressRepository.findByCircleId(circleId).stream()
                    .filter(p -> childIds.contains(p.getStudent().getId()))
                    .toList();
            return toPage(filtered, pageable).map(this::toResponse);
        }

        mosqueAccessService.assertCanAccessMosque(callerId, circle.getMosque().getId());

        if (role == UserRole.STUDENT) {
            Student self = studentRepository.findByUserIdAndStatus(callerId, EnrollmentStatus.ACTIVE).stream()
                    .findFirst()
                    .orElseGet(() -> studentRepository.findByUserId(callerId).stream()
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", callerId)));
            List<MemorizationProgress> own = memorizationProgressRepository
                    .findByStudentIdAndCircleId(self.getId(), circleId);
            return toPage(own, pageable).map(this::toResponse);
        }

        // TEACHER / MOSQUE_ADMIN / SUPER_ADMIN — full circle
        return memorizationProgressRepository.findByCircleId(circleId, pageable).map(this::toResponse);
    }

    /**
     * Student self-report: force studentId from token, attribute teacher from the circle,
     * and strip staff assessment fields so students cannot forge grades.
     */
    @Transactional
    public MemorizationProgressResponse createMyProgress(UUID callerId, MemorizationProgressCreateRequest request) {
        Student student = studentRepository.findByUserId(callerId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", callerId));
        if (request.getCircleId() == null) {
            throw new BadRequestException("circleId is required");
        }
        if (request.getSurahNumber() == null || request.getAyahFrom() == null || request.getAyahTo() == null
                || request.getSessionDate() == null) {
            throw new BadRequestException("surahNumber, ayahFrom, ayahTo, and sessionDate are required");
        }

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCircleId(student.getId(), request.getCircleId())
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("No active enrollment found for this circle"));

        Circle circle = enrollment.getCircle() != null
                ? enrollment.getCircle()
                : circleRepository.findById(request.getCircleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        Teacher circleTeacher = circle.getTeacher();
        if (circleTeacher == null || circleTeacher.getId() == null) {
            throw new BadRequestException("Circle has no assigned teacher");
        }

        request.setStudentId(student.getId());
        request.setTeacherId(circleTeacher.getId());
        // Entity requires grade; default when omitted. Strip staff-only notes/scores.
        if (request.getGrade() == null) {
            request.setGrade(RecitationGrade.ACCEPTABLE);
        }
        request.setTajweedScore(null);
        request.setTeacherNotes(null);

        return create(callerId, request);
    }

    @Transactional
    public MemorizationProgressResponse create(UUID callerId, MemorizationProgressCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
        if (student.getUser().getId().equals(callerId)) {
            if (student.getStatus() != EnrollmentStatus.ACTIVE) {
                throw new ForbiddenException("Access denied to mutate this student");
            }
        } else {
            mosqueAccessService.assertCanMutateStudent(callerId, student);
        }
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));

        // Verify active enrollment with optimistic locking to prevent TOCTOU race
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCircleId(request.getStudentId(), request.getCircleId())
                .orElseThrow(() -> new BadRequestException("Student is not enrolled in this circle"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Student enrollment is not active");
        }

        MemorizationProgress progress = MemorizationProgress.builder()
                .student(student)
                .circle(circle)
                .teacher(teacher)
                .surahNumber(request.getSurahNumber())
                .ayahFrom(request.getAyahFrom())
                .ayahTo(request.getAyahTo())
                .grade(request.getGrade())
                .tajweedScore(request.getTajweedScore())
                .teacherNotes(request.getTeacherNotes())
                .audioUrl(request.getAudioUrl())
                .sessionDate(request.getSessionDate())
                .build();

        return toResponse(memorizationProgressRepository.save(progress));
    }

    @Transactional
    public MemorizationProgressResponse update(UUID callerId, UUID id, MemorizationProgressUpdateRequest request) {
        MemorizationProgress progress = findEntityOrThrow(id);
        mosqueAccessService.assertCanMutateStudent(callerId, progress.getStudent().getId());

        if (request.getGrade() != null) {
            progress.setGrade(request.getGrade());
        }
        if (request.getTajweedScore() != null) {
            progress.setTajweedScore(request.getTajweedScore());
        }
        if (request.getTeacherNotes() != null) {
            progress.setTeacherNotes(request.getTeacherNotes());
        }
        if (request.getAudioUrl() != null) {
            progress.setAudioUrl(request.getAudioUrl());
        }

        return toResponse(memorizationProgressRepository.save(progress));
    }

    @Transactional
    public void delete(UUID callerId, UUID id) {
        MemorizationProgress progress = findEntityOrThrow(id);
        mosqueAccessService.assertCanMutateStudent(callerId, progress.getStudent().getId());
        memorizationProgressRepository.deleteById(id);
    }

    private UserRole findCallerRole(UUID callerId) {
        return userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId))
                .getRole();
    }

    private Set<UUID> linkedChildIds(UUID parentUserId) {
        return parentStudentRepository.findByParentId(parentUserId).stream()
                .map(ParentStudent::getStudent)
                .map(Student::getId)
                .collect(Collectors.toSet());
    }

    private static <T> Page<T> toPage(List<T> items, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }

    private MemorizationProgress findEntityOrThrow(UUID id) {
        return memorizationProgressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MemorizationProgress", "id", id));
    }

    private MemorizationProgressResponse toResponse(MemorizationProgress progress) {
        return MemorizationProgressResponse.builder()
                .id(progress.getId())
                .studentId(progress.getStudent().getId())
                .circleId(progress.getCircle().getId())
                .teacherId(progress.getTeacher().getId())
                .surahNumber(progress.getSurahNumber())
                .ayahFrom(progress.getAyahFrom())
                .ayahTo(progress.getAyahTo())
                .grade(progress.getGrade())
                .tajweedScore(progress.getTajweedScore())
                .teacherNotes(progress.getTeacherNotes())
                .audioUrl(progress.getAudioUrl())
                .sessionDate(progress.getSessionDate())
                .createdAt(progress.getCreatedAt())
                .build();
    }
}
