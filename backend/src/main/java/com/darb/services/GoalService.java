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
public class GoalService {

    private final GoalRepository goalRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final UserRepository userRepository;
    private final MosqueAccessService mosqueAccessService;

    @Transactional(readOnly = true)
    public Page<GoalResponse> findAll(Pageable pageable) {
        return goalRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GoalResponse findById(UUID callerId, UUID id) {
        Goal goal = findEntityOrThrow(id);
        UUID authorId = goal.getSetBy().getId();
        if (!authorId.equals(callerId)) {
            mosqueAccessService.assertCanAccessStudent(callerId, goal.getStudent().getId());
        }
        return toResponse(goal);
    }

    @Transactional(readOnly = true)
    public Page<GoalResponse> findByStudentId(UUID callerId, UUID studentId, Pageable pageable) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        return goalRepository.findByStudentId(studentId, pageable).map(this::toResponse);
    }

    @Transactional
    public GoalResponse create(UUID callerId, GoalCreateRequest request) {
        mosqueAccessService.assertCanMutateStudent(callerId, request.getStudentId());
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
    public GoalResponse update(UUID callerId, UUID id, GoalUpdateRequest request) {
        Goal goal = findEntityOrThrow(id);
        mosqueAccessService.assertCanMutateStudent(callerId, goal.getStudent().getId());

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
    public void delete(UUID callerId, UUID id) {
        Goal goal = findEntityOrThrow(id);
        mosqueAccessService.assertCanMutateStudent(callerId, goal.getStudent().getId());
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
