package com.darb.services;

import com.darb.dtos.goal.GoalCreateRequest;
import com.darb.dtos.goal.GoalResponse;
import com.darb.dtos.goal.GoalUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Goal;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.GoalRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
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
public class GoalService {

    private final GoalRepository goalRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<GoalResponse> findAll(Pageable pageable) {
        return goalRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GoalResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<GoalResponse> findByStudentId(UUID studentId, Pageable pageable) {
        return goalRepository.findByStudentId(studentId, pageable).map(this::toResponse);
    }

    @Transactional
    public GoalResponse create(GoalCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        User setBy = userRepository.findById(request.getSetBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getSetBy()));

        Goal goal = Goal.builder()
                .student(student)
                .circle(circle)
                .title(request.getTitle())
                .targetSurah(request.getTargetSurah())
                .targetJuz(request.getTargetJuz())
                .status(request.getStatus())
                .dueDate(request.getDueDate())
                .setBy(setBy)
                .build();

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse update(UUID id, GoalUpdateRequest request) {
        Goal goal = findEntityOrThrow(id);

        if (request.getTitle() != null) {
            goal.setTitle(request.getTitle());
        }
        if (request.getTargetSurah() != null) {
            goal.setTargetSurah(request.getTargetSurah());
        }
        if (request.getTargetJuz() != null) {
            goal.setTargetJuz(request.getTargetJuz());
        }
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }
        if (request.getDueDate() != null) {
            goal.setDueDate(request.getDueDate());
        }
        if (request.getCompletedDate() != null) {
            goal.setCompletedDate(request.getCompletedDate());
        }

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public void delete(UUID id) {
        goalRepository.deleteById(id);
    }

    private Goal findEntityOrThrow(UUID id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", id));
    }

    private GoalResponse toResponse(Goal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .studentId(goal.getStudent().getId())
                .circleId(goal.getCircle().getId())
                .title(goal.getTitle())
                .targetSurah(goal.getTargetSurah())
                .targetJuz(goal.getTargetJuz())
                .status(goal.getStatus())
                .dueDate(goal.getDueDate())
                .completedDate(goal.getCompletedDate())
                .setBy(goal.getSetBy().getId())
                .createdAt(goal.getCreatedAt())
                .build();
    }
}
