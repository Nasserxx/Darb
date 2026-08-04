package com.darb.services;

import com.darb.dtos.memorization.MemorizationProgressCreateRequest;
import com.darb.dtos.memorization.MemorizationProgressResponse;
import com.darb.dtos.memorization.MemorizationProgressUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.MemorizationProgress;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.MemorizationProgressRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemorizationProgressService {

    private final MemorizationProgressRepository memorizationProgressRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
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
        mosqueAccessService.assertCanAccessMosque(callerId, circle.getMosque().getId());
        return memorizationProgressRepository.findByCircleId(circleId, pageable).map(this::toResponse);
    }

    @Transactional
    public MemorizationProgressResponse create(MemorizationProgressCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
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
        mosqueAccessService.assertCanAccessStudent(callerId, progress.getStudent().getId());

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
    public void delete(UUID id) {
        memorizationProgressRepository.deleteById(id);
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
