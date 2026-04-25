package com.darb.services;

import com.darb.dtos.memorization.MemorizationProgressCreateRequest;
import com.darb.dtos.memorization.MemorizationProgressResponse;
import com.darb.dtos.memorization.MemorizationProgressUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.MemorizationProgress;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.MemorizationProgressRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
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

    @Transactional(readOnly = true)
    public Page<MemorizationProgressResponse> findAll(Pageable pageable) {
        return memorizationProgressRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MemorizationProgressResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<MemorizationProgressResponse> findByStudentId(UUID studentId, Pageable pageable) {
        return memorizationProgressRepository.findByStudentId(studentId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MemorizationProgressResponse> findByCircleId(UUID circleId, Pageable pageable) {
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
    public MemorizationProgressResponse update(UUID id, MemorizationProgressUpdateRequest request) {
        MemorizationProgress progress = findEntityOrThrow(id);

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
