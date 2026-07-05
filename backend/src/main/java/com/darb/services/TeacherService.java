package com.darb.services;

import com.darb.dtos.teacher.TeacherCreateRequest;
import com.darb.dtos.teacher.TeacherResponse;
import com.darb.dtos.teacher.TeacherUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.darb.entities.enums.JoinRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;
    private final MosqueMemberJoinRequestRepository joinRequestRepository;
    private final MosqueAccessService mosqueAccessService;

    @Transactional(readOnly = true)
    public Page<TeacherResponse> findAll(UUID callerId, Pageable pageable) {
        return mosqueAccessService.pageForCaller(
                callerId,
                pageable,
                mosqueId -> teacherRepository.findByMosqueId(mosqueId, pageable),
                teacherRepository::findAll
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(UUID callerId, UUID id) {
        Teacher teacher = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessTeacher(callerId, teacher);
        return toResponse(teacher);
    }

    @Transactional
    public TeacherResponse create(UUID callerId, TeacherCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        mosqueAccessService.assertCanAccessMosque(callerId, mosque.getId());

        Teacher teacher = Teacher.builder()
                .user(user)
                .mosque(mosque)
                .specialization(request.getSpecialization())
                .bio(request.getBio())
                .yearsExperience(request.getYearsExperience())
                .ijazahChain(request.getIjazahChain())
                .isAvailable(true)
                .isActive(true)
                .joinedAt(Instant.now())
                .build();

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse joinByInviteCode(UUID userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.TEACHER) {
            throw new ForbiddenException("Only teachers can use this endpoint");
        }
        if (!teacherRepository.findByUserId(userId).isEmpty()) {
            throw new ForbiddenException("You already have a teacher profile");
        }
        if (joinRequestRepository.existsByUserIdAndStatus(userId, JoinRequestStatus.PENDING)) {
            throw new ForbiddenException("You already have a pending join request");
        }

        String normalizedCode = inviteCode == null ? "" : inviteCode.trim();
        if (normalizedCode.isBlank()) {
            throw new com.darb.exceptions.BadRequestException("Invite code is required");
        }

        Mosque mosque = mosqueRepository.findActiveByTeacherInviteCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "inviteCode", normalizedCode));

        return onboard(userId, mosque.getId());
    }

    @Transactional
    public TeacherResponse onboard(UUID userId, UUID mosqueId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getRole() != UserRole.TEACHER) {
            throw new ForbiddenException("Only teachers can use this endpoint");
        }
        if (!teacherRepository.findByUserId(userId).isEmpty()) {
            throw new ForbiddenException("You already have a teacher profile");
        }
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));

        Teacher teacher = Teacher.builder()
                .user(user)
                .mosque(mosque)
                .isAvailable(true)
                .isActive(true)
                .joinedAt(Instant.now())
                .build();

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(UUID callerId, UUID id, TeacherUpdateRequest request) {
        Teacher teacher = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessTeacher(callerId, teacher);

        if (request.getSpecialization() != null) {
            teacher.setSpecialization(request.getSpecialization());
        }
        if (request.getBio() != null) {
            teacher.setBio(request.getBio());
        }
        if (request.getYearsExperience() != null) {
            teacher.setYearsExperience(request.getYearsExperience());
        }
        if (request.getIjazahChain() != null) {
            teacher.setIjazahChain(request.getIjazahChain());
        }
        if (request.getIsAvailable() != null) {
            teacher.setIsAvailable(request.getIsAvailable());
        }

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public void delete(UUID callerId, UUID id) {
        Teacher teacher = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessTeacher(callerId, teacher);
        teacher.setIsActive(false);
        teacherRepository.save(teacher);
    }

    private Teacher findEntityOrThrow(UUID id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
    }

    private TeacherResponse toResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUser().getId())
                .mosqueId(teacher.getMosque().getId())
                .specialization(teacher.getSpecialization())
                .bio(teacher.getBio())
                .yearsExperience(teacher.getYearsExperience())
                .ijazahChain(teacher.getIjazahChain())
                .isAvailable(teacher.getIsAvailable())
                .isActive(teacher.getIsActive())
                .joinedAt(teacher.getJoinedAt())
                .build();
    }
}
