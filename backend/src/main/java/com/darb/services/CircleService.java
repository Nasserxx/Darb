package com.darb.services;

import com.darb.dtos.circle.CircleCreateRequest;
import com.darb.dtos.circle.CircleResponse;
import com.darb.dtos.circle.CircleUpdateRequest;
import com.darb.entities.Circle;
import com.darb.entities.Mosque;
import com.darb.entities.Teacher;
import com.darb.entities.enums.CircleStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.TeacherRepository;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CircleService {

    private final CircleRepository circleRepository;
    private final MosqueRepository mosqueRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final MosqueAccessService mosqueAccessService;
    private final OverrideAuditService overrideAuditService;

    @Transactional(readOnly = true)
    public Page<CircleResponse> findAll(UUID callerId, Pageable pageable, UUID mosqueIdFilter, String q) {
        UserRole role = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId))
                .getRole();

        if (role == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(callerId).stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", callerId));
            return circleRepository.findByTeacherId(teacher.getId(), pageable)
                    .map(this::toResponse);
        }

        mosqueAccessService.assertValidNameFilter(callerId, mosqueIdFilter, q);
        if (mosqueAccessService.shouldReturnEmptyListPage(callerId)) {
            return Page.empty(pageable);
        }
        UUID mosqueId = mosqueAccessService.resolveEffectiveMosqueIdForList(callerId, mosqueIdFilter);
        Specification<Circle> spec = (root, query, cb) -> cb.conjunction();
        if (mosqueId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mosque").get("id"), mosqueId));
        }
        spec = NameFilterSpecs.and(spec, NameFilterSpecs.circleNameLike(q));
        return circleRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CircleResponse findById(UUID callerId, UUID id) {
        Circle circle = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessMosque(callerId, circle.getMosque().getId());
        return toResponse(circle);
    }

    @Transactional
    public CircleResponse create(UUID callerId, CircleCreateRequest request) {
        UUID mosqueId = mosqueAccessService.resolveMosqueIdForAdminMutation(callerId, request.getMosqueId());
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getTeacherId()));
        assertTeacherBelongsToMosque(teacher, mosqueId);

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
    public CircleResponse update(UUID callerId, UUID id, CircleUpdateRequest request) {
        Circle circle = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessMosque(callerId, circle.getMosque().getId());

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
    public void delete(UUID callerId, UUID id, String auditReasonHeader, String auditReasonBody) {
        Circle circle = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessMosque(callerId, circle.getMosque().getId());
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    circle.getMosque().getId(),
                    "CIRCLE_DEACTIVATE",
                    auditReason,
                    "Circle",
                    circle.getId());
        }
        circle.setStatus(CircleStatus.ENDED);
        circleRepository.save(circle);
    }

    private void assertTeacherBelongsToMosque(Teacher teacher, UUID resolvedMosqueId) {
        if (!teacher.getMosque().getId().equals(resolvedMosqueId)) {
            throw new BadRequestException("Teacher must belong to the same mosque");
        }
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
                .teacherName(circle.getTeacher().getUser().getFullName())
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

