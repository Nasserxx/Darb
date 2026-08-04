package com.darb.services;

import com.darb.dtos.mosque.MemberJoinRequestCreateRequest;
import com.darb.dtos.mosque.MemberJoinRequestResponse;
import com.darb.entities.Mosque;
import com.darb.entities.MosqueMemberJoinRequest;
import com.darb.entities.User;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MosqueMemberJoinRequestService {

    private final MosqueMemberJoinRequestRepository joinRequestRepository;
    private final MosqueRepository mosqueRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final MosqueAccessService mosqueAccessService;

    @Transactional
    public MemberJoinRequestResponse create(UUID userId, MemberJoinRequestCreateRequest request) {
        User user = findUserOrThrow(userId);
        assertEligibleForJoinRequest(user);

        if (joinRequestRepository.existsByUserIdAndStatus(userId, JoinRequestStatus.PENDING)) {
            throw new ForbiddenException("You already have a pending join request");
        }

        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        if (!Boolean.TRUE.equals(mosque.getIsActive())) {
            throw new BadRequestException("Mosque is not active");
        }

        MosqueMemberJoinRequest joinRequest = MosqueMemberJoinRequest.builder()
                .user(user)
                .mosque(mosque)
                .requestedRole(user.getRole())
                .status(JoinRequestStatus.PENDING)
                .build();

        return toResponse(joinRequestRepository.save(joinRequest));
    }

    @Transactional(readOnly = true)
    public List<MemberJoinRequestResponse> listPendingForAdmin(UUID adminUserId) {
        UUID mosqueId = mosqueAccessService.requireMosqueIdForAdmin(adminUserId);
        return joinRequestRepository.findByMosqueIdAndStatus(mosqueId, JoinRequestStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MemberJoinRequestResponse approve(UUID adminUserId, UUID requestId) {
        MosqueMemberJoinRequest joinRequest = findEntityOrThrow(requestId);
        mosqueAccessService.assertCanAccessMosque(adminUserId, joinRequest.getMosque().getId());

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not pending");
        }

        UUID memberUserId = joinRequest.getUser().getId();
        UUID mosqueId = joinRequest.getMosque().getId();

        if (joinRequest.getRequestedRole() == UserRole.TEACHER) {
            teacherService.onboard(memberUserId, mosqueId);
        } else if (joinRequest.getRequestedRole() == UserRole.STUDENT) {
            studentService.onboard(memberUserId, mosqueId);
        } else {
            throw new BadRequestException("Unsupported join request role");
        }

        joinRequest.setStatus(JoinRequestStatus.APPROVED);
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setReviewedBy(findUserOrThrow(adminUserId));
        return toResponse(joinRequestRepository.save(joinRequest));
    }

    @Transactional
    public MemberJoinRequestResponse reject(UUID adminUserId, UUID requestId) {
        MosqueMemberJoinRequest joinRequest = findEntityOrThrow(requestId);
        mosqueAccessService.assertCanAccessMosque(adminUserId, joinRequest.getMosque().getId());

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not pending");
        }

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setReviewedBy(findUserOrThrow(adminUserId));
        return toResponse(joinRequestRepository.save(joinRequest));
    }

    @Transactional
    public void cancelMyJoinRequest(UUID userId) {
        MosqueMemberJoinRequest joinRequest = joinRequestRepository.findByUserIdAndStatus(
                        userId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("MosqueJoinRequest", "userId", userId));

        joinRequest.setStatus(JoinRequestStatus.CANCELLED);
        joinRequestRepository.save(joinRequest);
    }

    private void assertEligibleForJoinRequest(User user) {
        if (user.getRole() != UserRole.TEACHER && user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Only teachers and students can request to join a mosque");
        }
        if (user.getRole() == UserRole.TEACHER && !teacherRepository.findByUserId(user.getId()).isEmpty()) {
            throw new ForbiddenException("You already have a teacher profile");
        }
        if (user.getRole() == UserRole.STUDENT && !studentRepository.findByUserId(user.getId()).isEmpty()) {
            throw new ForbiddenException("You already have a student profile");
        }
    }

    private MosqueMemberJoinRequest findEntityOrThrow(UUID id) {
        return joinRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MosqueJoinRequest", "id", id));
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private MemberJoinRequestResponse toResponse(MosqueMemberJoinRequest joinRequest) {
        return MemberJoinRequestResponse.builder()
                .id(joinRequest.getId())
                .userId(joinRequest.getUser().getId())
                .userFullName(joinRequest.getUser().getFullName())
                .userEmail(joinRequest.getUser().getEmail())
                .mosqueId(joinRequest.getMosque().getId())
                .mosqueName(joinRequest.getMosque().getName())
                .requestedRole(joinRequest.getRequestedRole())
                .status(joinRequest.getStatus())
                .createdAt(joinRequest.getCreatedAt())
                .reviewedAt(joinRequest.getReviewedAt())
                .build();
    }
}
