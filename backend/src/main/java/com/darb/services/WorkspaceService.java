package com.darb.services;

import com.darb.dtos.workspace.WorkspaceProfileResponse;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.MosqueMemberJoinRequest;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.MembershipStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final UserRepository userRepository;
    private final MosqueAdminRepository mosqueAdminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final MosqueMemberJoinRequestRepository joinRequestRepository;

    public WorkspaceProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return switch (user.getRole()) {
            case SUPER_ADMIN -> WorkspaceProfileResponse.builder()
                    .profileId(userId)
                    .membershipStatus(MembershipStatus.ASSIGNED)
                    .build();
            case MOSQUE_ADMIN -> fromMosqueAdmin(userId);
            case TEACHER -> fromTeacher(userId);
            case STUDENT -> fromStudent(userId);
            case PARENT -> fromParent(userId);
        };
    }

    private WorkspaceProfileResponse fromMosqueAdmin(UUID userId) {
        Optional<MosqueAdmin> admin = mosqueAdminRepository.findByUserId(userId).stream().findFirst();
        if (admin.isPresent()) {
            MosqueAdmin assignment = admin.get();
            return WorkspaceProfileResponse.builder()
                    .profileId(assignment.getId())
                    .mosqueId(assignment.getMosque().getId())
                    .mosqueName(assignment.getMosque().getName())
                    .membershipStatus(MembershipStatus.ASSIGNED)
                    .build();
        }
        throw new ResourceNotFoundException("MosqueAdmin", "userId", userId);
    }

    private WorkspaceProfileResponse fromTeacher(UUID userId) {
        Optional<Teacher> teacher = teacherRepository.findByUserId(userId).stream().findFirst();
        if (teacher.isPresent()) {
            Teacher profile = teacher.get();
            return WorkspaceProfileResponse.builder()
                    .profileId(profile.getId())
                    .mosqueId(profile.getMosque().getId())
                    .mosqueName(profile.getMosque().getName())
                    .teacherId(profile.getId())
                    .membershipStatus(MembershipStatus.ASSIGNED)
                    .build();
        }
        return fromPendingJoinRequest(userId, UserRole.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", userId));
    }

    private WorkspaceProfileResponse fromStudent(UUID userId) {
        Optional<Student> student = studentRepository.findByUserId(userId).stream().findFirst();
        if (student.isPresent()) {
            Student profile = student.get();
            return WorkspaceProfileResponse.builder()
                    .profileId(profile.getId())
                    .mosqueId(profile.getMosque().getId())
                    .mosqueName(profile.getMosque().getName())
                    .studentId(profile.getId())
                    .membershipStatus(MembershipStatus.ASSIGNED)
                    .build();
        }
        return fromPendingJoinRequest(userId, UserRole.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
    }

    private WorkspaceProfileResponse fromParent(UUID userId) {
        List<ParentStudent> links = parentStudentRepository.findByParentId(userId);
        if (links.isEmpty()) {
            throw new ResourceNotFoundException("ParentStudent", "parentId", userId);
        }
        List<UUID> studentIds = links.stream()
                .map(link -> link.getStudent().getId())
                .toList();
        return WorkspaceProfileResponse.builder()
                .profileId(userId)
                .parentStudentIds(studentIds)
                .membershipStatus(MembershipStatus.ASSIGNED)
                .build();
    }

    private Optional<WorkspaceProfileResponse> fromPendingJoinRequest(UUID userId, UserRole role) {
        return joinRequestRepository
                .findByUserIdAndStatusAndRequestedRole(userId, JoinRequestStatus.PENDING, role)
                .map(this::toPendingProfile);
    }

    private WorkspaceProfileResponse toPendingProfile(MosqueMemberJoinRequest joinRequest) {
        return WorkspaceProfileResponse.builder()
                .profileId(joinRequest.getId())
                .membershipStatus(MembershipStatus.PENDING)
                .pendingMosqueName(joinRequest.getMosque().getName())
                .build();
    }
}
