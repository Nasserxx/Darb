package com.darb.security;

import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.Teacher;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.TeacherRepository;
import com.darb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class MosqueAccessService {

    private final UserRepository userRepository;
    private final MosqueAdminRepository mosqueAdminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;

    public UUID resolveCallerMosqueId(UUID userId, UserRole role) {
        return switch (role) {
            case SUPER_ADMIN, PARENT -> null;
            case MOSQUE_ADMIN -> mosqueAdminRepository.findByUserId(userId).stream()
                    .findFirst()
                    .map(admin -> admin.getMosque().getId())
                    .orElse(null);
            case TEACHER -> teacherRepository.findByUserId(userId).stream()
                    .findFirst()
                    .map(teacher -> teacher.getMosque().getId())
                    .orElse(null);
            case STUDENT -> studentRepository.findByUserId(userId).stream()
                    .findFirst()
                    .map(student -> student.getMosque().getId())
                    .orElse(null);
        };
    }

    public void assertCanAccessMosque(UUID callerId, UUID mosqueId) {
        UserRole role = findUserRole(callerId);
        if (role == UserRole.SUPER_ADMIN) {
            return;
        }
        UUID callerMosqueId = resolveCallerMosqueId(callerId, role);
        if (callerMosqueId == null || !callerMosqueId.equals(mosqueId)) {
            throw new ForbiddenException("Access denied to this mosque");
        }
    }

    public UUID requireMosqueIdForAdmin(UUID adminUserId) {
        UserRole role = findUserRole(adminUserId);
        if (role == UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Super admins do not have a single mosque context");
        }
        UUID mosqueId = resolveCallerMosqueId(adminUserId, role);
        if (mosqueId == null) {
            throw new ForbiddenException("No mosque assignment found");
        }
        return mosqueId;
    }

    public void requireMosqueAdminWith(UUID callerId, UUID mosqueId) {
        UserRole role = findUserRole(callerId);
        if (role == UserRole.SUPER_ADMIN) {
            return;
        }
        if (role == UserRole.MOSQUE_ADMIN && mosqueAdminRepository.existsByUserIdAndMosqueId(callerId, mosqueId)) {
            return;
        }
        throw new ForbiddenException("Access denied to this mosque");
    }

    public void assertCanAccessStudent(UUID callerId, UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        assertCanAccessStudent(callerId, student);
    }

    public void assertCanAccessStudent(UUID callerId, Student student) {
        UserRole role = findUserRole(callerId);
        if (role == UserRole.SUPER_ADMIN) {
            return;
        }
        if (role == UserRole.STUDENT && student.getUser().getId().equals(callerId)) {
            return;
        }
        if (role == UserRole.MOSQUE_ADMIN || role == UserRole.TEACHER) {
            assertCanAccessMosque(callerId, student.getMosque().getId());
            return;
        }
        if (role == UserRole.PARENT) {
            ParentStudent link = parentStudentRepository.findByParentId(callerId).stream()
                    .filter(ps -> ps.getStudent().getId().equals(student.getId()))
                    .findFirst()
                    .orElse(null);
            if (link != null) {
                UUID linkedMosqueId = link.getMosque() != null ? link.getMosque().getId() : null;
                UUID currentMosqueId = student.getMosque().getId();
                if (linkedMosqueId != null && !linkedMosqueId.equals(currentMosqueId)) {
                    throw new ForbiddenException("Parent cannot access student from a different mosque");
                }
                return;
            }
        }
        throw new ForbiddenException("Access denied to this student");
    }

    public void assertCanAccessParentStudent(UUID callerId, ParentStudent link) {
        UserRole role = findUserRole(callerId);
        if (role == UserRole.SUPER_ADMIN) {
            return;
        }
        if (role == UserRole.PARENT) {
            if (!link.getParent().getId().equals(callerId)) {
                throw new ForbiddenException("Access denied to this parent-student link");
            }
            return;
        }
        if (role == UserRole.MOSQUE_ADMIN || role == UserRole.TEACHER) {
            assertCanAccessMosque(callerId, link.getStudent().getMosque().getId());
            return;
        }
        throw new ForbiddenException("Access denied to this parent-student link");
    }

    public void assertCanAccessTeacher(UUID callerId, Teacher teacher) {
        UserRole role = findUserRole(callerId);
        if (role == UserRole.SUPER_ADMIN) {
            return;
        }
        if (role == UserRole.TEACHER && teacher.getUser().getId().equals(callerId)) {
            return;
        }
        if (role == UserRole.MOSQUE_ADMIN) {
            assertCanAccessMosque(callerId, teacher.getMosque().getId());
            return;
        }
        throw new ForbiddenException("Access denied to this teacher");
    }

    public void assertCanAccessUser(UUID callerId, UUID targetUserId) {
        if (callerId.equals(targetUserId)) {
            return;
        }
        UserRole role = findUserRole(callerId);
        if (role == UserRole.SUPER_ADMIN) {
            return;
        }
        if (role == UserRole.MOSQUE_ADMIN) {
            UUID mosqueId = resolveCallerMosqueId(callerId, role);
            if (mosqueId != null && isUserInMosque(targetUserId, mosqueId)) {
                return;
            }
        }
        throw new ForbiddenException("Access denied to this user");
    }

    private boolean isUserInMosque(UUID userId, UUID mosqueId) {
        boolean isStudent = studentRepository.findByUserId(userId).stream()
                .anyMatch(student -> student.getMosque().getId().equals(mosqueId));
        if (isStudent) {
            return true;
        }
        boolean isTeacher = teacherRepository.findByUserId(userId).stream()
                .anyMatch(teacher -> teacher.getMosque().getId().equals(mosqueId));
        if (isTeacher) {
            return true;
        }
        return mosqueAdminRepository.findByUserId(userId).stream()
                .anyMatch(admin -> admin.getMosque().getId().equals(mosqueId));
    }

    public <T> Page<T> pageForCaller(
            UUID callerId,
            Pageable pageable,
            Function<UUID, Page<T>> findByMosqueId,
            Function<Pageable, Page<T>> findAll) {
        UserRole role = findUserRole(callerId);
        UUID mosqueId = resolveCallerMosqueId(callerId, role);
        if (mosqueId != null) {
            return findByMosqueId.apply(mosqueId);
        }
        if (role == UserRole.SUPER_ADMIN) {
            return findAll.apply(pageable);
        }
        return Page.empty(pageable);
    }

    private UserRole findUserRole(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId))
                .getRole();
    }
}
