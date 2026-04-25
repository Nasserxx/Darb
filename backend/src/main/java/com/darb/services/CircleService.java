package com.darb.services;

import com.darb.dtos.circle.CircleCreateRequest;
import com.darb.dtos.circle.CircleResponse;
import com.darb.dtos.circle.CircleUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Mosque;
import com.darb.entities.Teacher;
import com.darb.entities.enums.CircleStatus;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.MosqueRepository;
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
public class CircleService {

    private final CircleRepository circleRepository;
    private final MosqueRepository mosqueRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public Page<CircleResponse> findAll(Pageable pageable) {
        return circleRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CircleResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public CircleResponse create(CircleCreateRequest request) {
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));

        Circle circle = Circle.builder()
                .mosque(mosque)
                .teacher(teacher)
                .name(request.getName())
                .level(request.getLevel())
                .type(request.getType())
                .status(request.getStatus() != null ? request.getStatus() : CircleStatus.PLANNING)
                .capacity(request.getCapacity())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .daysOfWeek(request.getDaysOfWeek())
                .roomOrLink(request.getRoomOrLink())
                .lateThresholdMinutes(request.getLateThresholdMinutes())
                .monthlyFee(request.getMonthlyFee())
                .build();

        return toResponse(circleRepository.save(circle));
    }

    @Transactional
    public CircleResponse update(UUID id, CircleUpdateRequest request) {
        Circle circle = findEntityOrThrow(id);

        if (request.getName() != null) {
            circle.setName(request.getName());
        }
        if (request.getLevel() != null) {
            circle.setLevel(request.getLevel());
        }
        if (request.getType() != null) {
            circle.setType(request.getType());
        }
        if (request.getStatus() != null) {
            circle.setStatus(request.getStatus());
        }
        if (request.getCapacity() != null) {
            circle.setCapacity(request.getCapacity());
        }
        if (request.getStartTime() != null) {
            circle.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            circle.setEndTime(request.getEndTime());
        }
        if (request.getDaysOfWeek() != null) {
            circle.setDaysOfWeek(request.getDaysOfWeek());
        }
        if (request.getRoomOrLink() != null) {
            circle.setRoomOrLink(request.getRoomOrLink());
        }
        if (request.getLateThresholdMinutes() != null) {
            circle.setLateThresholdMinutes(request.getLateThresholdMinutes());
        }
        if (request.getMonthlyFee() != null) {
            circle.setMonthlyFee(request.getMonthlyFee());
        }

        return toResponse(circleRepository.save(circle));
    }

    @Transactional
    public void delete(UUID id) {
        Circle circle = findEntityOrThrow(id);
        circle.setStatus(CircleStatus.ENDED);
        circleRepository.save(circle);
    }

    private Circle findEntityOrThrow(UUID id) {
        return circleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", id));
    }

    private CircleResponse toResponse(Circle circle) {
        return CircleResponse.builder()
                .id(circle.getId())
                .mosqueId(circle.getMosque().getId())
                .teacherId(circle.getTeacher().getId())
                .name(circle.getName())
                .level(circle.getLevel())
                .type(circle.getType())
                .status(circle.getStatus())
                .capacity(circle.getCapacity())
                .startTime(circle.getStartTime())
                .endTime(circle.getEndTime())
                .daysOfWeek(circle.getDaysOfWeek())
                .roomOrLink(circle.getRoomOrLink())
                .lateThresholdMinutes(circle.getLateThresholdMinutes())
                .monthlyFee(circle.getMonthlyFee())
                .createdAt(circle.getCreatedAt())
                .build();
    }
}
