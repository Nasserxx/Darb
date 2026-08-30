package com.darb.services;

import com.darb.dtos.mosque.MemberJoinRequestCreateRequest;
import com.darb.dtos.mosque.MemberJoinRequestResponse;
import com.darb.dtos.notification.NotificationCreateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.MosqueMemberJoinRequest;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.JoinRequestDirection;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.NotificationChannel;
import com.darb.entities.enums.NotificationStatus;
import com.darb.entities.enums.ParentRelationship;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MosqueMemberJoinRequestService {

    private static final String SLOT_TAKEN =
            "User is already a member or has a pending invitation for this mosque and role";

    private final MosqueMemberJoinRequestRepository joinRequestRepository;
    private final MosqueRepository mosqueRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final MosqueAdminRepository mosqueAdminRepository;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final MosqueAccessService mosqueAccessService;
    private final NotificationService notificationService;
    private final OverrideAuditService overrideAuditService;

    @Transactional
    public MemberJoinRequestResponse create(UUID userId, MemberJoinRequestCreateRequest request) {
        User user = findUserOrThrow(userId);
        if (user.getRole() != UserRole.TEACHER && user.getRole() != UserRole.STUDENT) {
            throw new ForbiddenException("Only teachers and students can request to join a mosque");
        }

        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        if (!Boolean.TRUE.equals(mosque.getIsActive())) {
            throw new BadRequestException("Mosque is not active");
        }

        assertSlotFree(user.getId(), mosque.getId(), user.getRole(), null);

        MosqueMemberJoinRequest joinRequest = MosqueMemberJoinRequest.builder()
                .user(user)
                .mosque(mosque)
                .requestedRole(user.getRole())
                .direction(JoinRequestDirection.MEMBER_REQUEST)
                .status(JoinRequestStatus.PENDING)
                .build();

        return toResponse(joinRequestRepository.save(joinRequest));
    }

    @Transactional
    public MemberJoinRequestResponse invite(
            UUID adminId,
            UUID userId,
            UUID mosqueId,
            UserRole requestedRole,
            UUID linkedStudentId,
            ParentRelationship relationship) {
        mosqueAccessService.assertCanAccessMosque(adminId, mosqueId);
        User user = findUserOrThrow(userId);
        if (user.getRole() != requestedRole) {
            throw new BadRequestException("Invited user role must match the requested role");
        }

        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", mosqueId));
        if (!Boolean.TRUE.equals(mosque.getIsActive())) {
            throw new BadRequestException("Mosque is not active");
        }

        Student linkedStudent = null;
        if (requestedRole == UserRole.PARENT) {
            if (linkedStudentId == null) {
                throw new BadRequestException("linkedStudentId is required for parent invitations");
            }
            linkedStudent = studentRepository.findById(linkedStudentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", linkedStudentId));
            if (!linkedStudent.getMosque().getId().equals(mosqueId)) {
                throw new BadRequestException("Student does not belong to this mosque");
            }
            mosqueAccessService.assertCanAccessStudent(adminId, linkedStudent);
        } else if (linkedStudentId != null) {
            throw new BadRequestException("linkedStudentId is only valid for parent invitations");
        }

        assertSlotFree(userId, mosqueId, requestedRole, linkedStudentId);

        MosqueMemberJoinRequest joinRequest = MosqueMemberJoinRequest.builder()
                .user(user)
                .mosque(mosque)
                .requestedRole(requestedRole)
                .direction(JoinRequestDirection.ADMIN_INVITE)
                .linkedStudent(linkedStudent)
                .relationship(relationship)
                .status(JoinRequestStatus.PENDING)
                .build();
        MosqueMemberJoinRequest saved = joinRequestRepository.save(joinRequest);

        sendInApp(
                mosque.getId(),
                user.getId(),
                adminId,
                "Invitation to join " + mosque.getName(),
                "You were invited to join " + mosque.getName() + " as " + requestedRole
                        + ". Open Invitations to accept or refuse.");
        log.info("Created ADMIN_INVITE {} mosque {} role {}", saved.getId(), mosqueId, requestedRole);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MemberJoinRequestResponse> listPendingForAdmin(UUID adminUserId) {
        UUID mosqueId = mosqueAccessService.requireMosqueIdForAdmin(adminUserId);
        return joinRequestRepository.findByMosqueIdAndStatus(mosqueId, JoinRequestStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberJoinRequestResponse> listMine(UUID userId) {
        return joinRequestRepository
                .findByUserIdAndDirectionAndStatus(userId, JoinRequestDirection.ADMIN_INVITE, JoinRequestStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MemberJoinRequestResponse accept(
            UUID callerId, UUID requestId, String auditReasonHeader, String auditReasonBody) {
        MosqueMemberJoinRequest joinRequest = findEntityOrThrow(requestId);
        assertInviteeOrSuperAdmin(callerId, joinRequest);
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        if (joinRequest.getDirection() != JoinRequestDirection.ADMIN_INVITE) {
            throw new BadRequestException("Only ADMIN_INVITE can be accepted");
        }
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not pending");
        }

        UUID memberUserId = joinRequest.getUser().getId();
        UUID mosqueId = joinRequest.getMosque().getId();

        joinRequest.setStatus(JoinRequestStatus.APPROVED);
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setReviewedBy(findUserOrThrow(callerId));
        joinRequestRepository.saveAndFlush(joinRequest);
        applyMembership(joinRequest, memberUserId, mosqueId);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId, mosqueId, "JOIN_FORCE_ACCEPT", auditReason, "MosqueJoinRequest", requestId);
        }
        log.info("Accepted ADMIN_INVITE {} mosque {}", requestId, mosqueId);
        return toResponse(joinRequest);
    }

    @Transactional
    public MemberJoinRequestResponse refuse(
            UUID callerId, UUID requestId, String auditReasonHeader, String auditReasonBody) {
        MosqueMemberJoinRequest joinRequest = findEntityOrThrow(requestId);
        assertInviteeOrSuperAdmin(callerId, joinRequest);
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        if (joinRequest.getDirection() != JoinRequestDirection.ADMIN_INVITE) {
            throw new BadRequestException("Only ADMIN_INVITE can be refused");
        }
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not pending");
        }

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setReviewedBy(findUserOrThrow(callerId));
        joinRequestRepository.save(joinRequest);

        String mosqueName = joinRequest.getMosque().getName();
        String fullName = joinRequest.getUser().getFullName();
        UserRole role = joinRequest.getRequestedRole();
        for (MosqueAdmin admin : mosqueAdminRepository.findByMosqueIdAndIsActiveTrue(joinRequest.getMosque().getId())) {
            sendInApp(
                    joinRequest.getMosque().getId(),
                    admin.getUser().getId(),
                    callerId,
                    "Invitation refused",
                    fullName + " refused to join " + mosqueName + " as " + role + ".");
        }
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    joinRequest.getMosque().getId(),
                    "JOIN_FORCE_CANCEL",
                    auditReason,
                    "MosqueJoinRequest",
                    requestId);
        }
        log.info("Refused ADMIN_INVITE {} mosque {}", requestId, joinRequest.getMosque().getId());
        return toResponse(joinRequest);
    }

    @Transactional
    public MemberJoinRequestResponse approve(
            UUID adminUserId, UUID requestId, String auditReasonHeader, String auditReasonBody) {
        MosqueMemberJoinRequest joinRequest = findEntityOrThrow(requestId);
        mosqueAccessService.assertCanAccessMosque(adminUserId, joinRequest.getMosque().getId());
        String auditReason = overrideAuditService.gateSuperAdmin(adminUserId, auditReasonHeader, auditReasonBody);

        if (joinRequest.getDirection() == JoinRequestDirection.ADMIN_INVITE) {
            throw new BadRequestException("Only the invited user can accept this invitation");
        }
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not pending");
        }

        UUID memberUserId = joinRequest.getUser().getId();
        UUID mosqueId = joinRequest.getMosque().getId();

        joinRequest.setStatus(JoinRequestStatus.APPROVED);
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setReviewedBy(findUserOrThrow(adminUserId));
        joinRequestRepository.saveAndFlush(joinRequest);
        applyMembership(joinRequest, memberUserId, mosqueId);
        sendDecisionNotification(adminUserId, joinRequest, "Join request approved",
                "Your request to join " + joinRequest.getMosque().getName() + " was approved.");
        if (auditReason != null) {
            overrideAuditService.record(
                    adminUserId, mosqueId, "JOIN_APPROVE", auditReason, "MosqueJoinRequest", requestId);
        }
        return toResponse(joinRequest);
    }

    @Transactional
    public MemberJoinRequestResponse reject(
            UUID adminUserId, UUID requestId, String auditReasonHeader, String auditReasonBody) {
        MosqueMemberJoinRequest joinRequest = findEntityOrThrow(requestId);
        mosqueAccessService.assertCanAccessMosque(adminUserId, joinRequest.getMosque().getId());
        String auditReason = overrideAuditService.gateSuperAdmin(adminUserId, auditReasonHeader, auditReasonBody);

        if (joinRequest.getDirection() == JoinRequestDirection.ADMIN_INVITE) {
            throw new BadRequestException("Only the invited user can refuse this invitation");
        }
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not pending");
        }

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setReviewedAt(Instant.now());
        joinRequest.setReviewedBy(findUserOrThrow(adminUserId));
        joinRequestRepository.save(joinRequest);
        sendDecisionNotification(adminUserId, joinRequest, "Join request rejected",
                "Your request to join " + joinRequest.getMosque().getName() + " was rejected.");
        if (auditReason != null) {
            overrideAuditService.record(
                    adminUserId,
                    joinRequest.getMosque().getId(),
                    "JOIN_REJECT",
                    auditReason,
                    "MosqueJoinRequest",
                    requestId);
        }
        return toResponse(joinRequest);
    }

    private void assertInviteeOrSuperAdmin(UUID callerId, MosqueMemberJoinRequest joinRequest) {
        if (joinRequest.getUser().getId().equals(callerId)) {
            return;
        }
        UserRole role = findUserOrThrow(callerId).getRole();
        if (role != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Access denied to this join request");
        }
    }

    @Transactional
    public void cancelMyJoinRequest(UUID userId) {
        List<MosqueMemberJoinRequest> pending = joinRequestRepository.findByUserIdAndDirectionAndStatus(
                userId, JoinRequestDirection.MEMBER_REQUEST, JoinRequestStatus.PENDING);
        if (pending.isEmpty()) {
            throw new ResourceNotFoundException("MosqueJoinRequest", "userId", userId);
        }
        for (MosqueMemberJoinRequest joinRequest : pending) {
            joinRequest.setStatus(JoinRequestStatus.CANCELLED);
        }
        joinRequestRepository.saveAll(pending);
    }

    private void applyMembership(MosqueMemberJoinRequest joinRequest, UUID memberUserId, UUID mosqueId) {
        if (joinRequest.getRequestedRole() == UserRole.TEACHER) {
            teacherService.completeApprovedJoin(memberUserId, mosqueId);
        } else if (joinRequest.getRequestedRole() == UserRole.STUDENT) {
            studentService.completeApprovedJoin(memberUserId, mosqueId);
        } else if (joinRequest.getRequestedRole() == UserRole.PARENT) {
            attachParent(joinRequest);
        } else {
            throw new BadRequestException("Unsupported join request role");
        }
    }

    private void attachParent(MosqueMemberJoinRequest joinRequest) {
        Student student = joinRequest.getLinkedStudent();
        if (student == null) {
            throw new BadRequestException("Parent invitation is missing linked student");
        }
        User parent = joinRequest.getUser();
        if (parentStudentRepository.existsByParent_IdAndStudent_Id(parent.getId(), student.getId())) {
            throw new BadRequestException("This parent is already linked to this student");
        }
        ParentRelationship relationship = joinRequest.getRelationship() != null
                ? joinRequest.getRelationship()
                : ParentRelationship.PARENT;
        parentStudentRepository.save(ParentStudent.builder()
                .parent(parent)
                .student(student)
                .mosque(student.getMosque())
                .relationship(relationship)
                .isPrimary(false)
                .receivesNotifications(true)
                .build());
    }

    private void assertSlotFree(UUID userId, UUID mosqueId, UserRole role, UUID linkedStudentId) {
        boolean live = switch (role) {
            case STUDENT -> studentRepository.existsByUserIdAndMosqueIdAndStatus(
                    userId, mosqueId, EnrollmentStatus.ACTIVE);
            case TEACHER -> teacherRepository.existsByUserIdAndMosqueIdAndIsActiveTrue(userId, mosqueId);
            case PARENT -> {
                boolean pairTaken = linkedStudentId != null
                        && parentStudentRepository.existsByParent_IdAndStudent_Id(userId, linkedStudentId);
                yield pairTaken || parentStudentRepository.existsByParent_IdAndStudent_Mosque_Id(userId, mosqueId);
            }
            default -> false;
        };
        boolean pending = joinRequestRepository.existsByUserIdAndMosqueIdAndRequestedRoleAndStatus(
                userId, mosqueId, role, JoinRequestStatus.PENDING);
        if (live || pending) {
            throw new BadRequestException(SLOT_TAKEN);
        }
    }

    private void sendDecisionNotification(UUID adminUserId, MosqueMemberJoinRequest joinRequest, String title, String body) {
        sendInApp(joinRequest.getMosque().getId(), joinRequest.getUser().getId(), adminUserId, title, body);
    }

    private void sendInApp(UUID mosqueId, UUID recipientUserId, UUID senderUserId, String title, String body) {
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setMosqueId(mosqueId);
        request.setRecipientUserId(recipientUserId);
        request.setSenderUserId(senderUserId);
        request.setTitle(title);
        request.setBody(body);
        request.setChannel(NotificationChannel.IN_APP);
        request.setStatus(NotificationStatus.PENDING);
        notificationService.create(request);
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
                .direction(joinRequest.getDirection())
                .linkedStudentId(joinRequest.getLinkedStudent() != null ? joinRequest.getLinkedStudent().getId() : null)
                .relationship(joinRequest.getRelationship())
                .createdAt(joinRequest.getCreatedAt())
                .reviewedAt(joinRequest.getReviewedAt())
                .build();
    }
}
