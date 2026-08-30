package com.darb.services;

import com.darb.dtos.attendance.AttendanceCreateRequest;
import com.darb.dtos.attendance.AttendanceExcuseRequest;
import com.darb.dtos.attendance.AttendanceResponse;
import com.darb.dtos.attendance.AttendanceUpdateRequest;
import com.darb.entities.Attendance;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.AbsenceReason;
import com.darb.entities.enums.AttendanceStatus;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.AttendanceRepository;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CircleRepository circleRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final MosqueAccessService mosqueAccessService;
    private final OverrideAuditService overrideAuditService;

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findAll(UUID callerId, Pageable pageable) {
        return mosqueAccessService.pageForCaller(
                callerId, pageable,
                mosqueId -> attendanceRepository.findByCircle_MosqueId(mosqueId, pageable),
                attendanceRepository::findAll
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceResponse findById(UUID callerId, UUID id) {
        Attendance attendance = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessStudent(callerId, attendance.getEnrollment().getStudent().getId());
        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findByCircleId(UUID callerId, UUID circleId, Pageable pageable) {
        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", circleId));
        UserRole role = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId))
                .getRole();

        if (role == UserRole.PARENT) {
            Set<UUID> childIds = parentStudentRepository.findByParentId(callerId).stream()
                    .map(ParentStudent::getStudent)
                    .map(Student::getId)
                    .collect(Collectors.toSet());
            if (childIds.isEmpty()) {
                return Page.empty(pageable);
            }
            List<Attendance> filtered = attendanceRepository.findByCircleId(circleId).stream()
                    .filter(a -> childIds.contains(a.getEnrollment().getStudent().getId()))
                    .toList();
            return toPage(filtered, pageable).map(this::toResponse);
        }

        mosqueAccessService.assertCanAccessMosque(callerId, circle.getMosque().getId());

        if (role == UserRole.STUDENT) {
            Student self = studentRepository.findByUserIdAndStatus(callerId, EnrollmentStatus.ACTIVE).stream()
                    .findFirst()
                    .orElseGet(() -> studentRepository.findByUserId(callerId).stream()
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", callerId)));
            List<Attendance> own = attendanceRepository.findByCircleId(circleId).stream()
                    .filter(a -> a.getEnrollment().getStudent().getId().equals(self.getId()))
                    .toList();
            return toPage(own, pageable).map(this::toResponse);
        }

        // TEACHER / MOSQUE_ADMIN / SUPER_ADMIN — full circle
        return attendanceRepository.findByCircleId(circleId, pageable).map(this::toResponse);
    }

    private static <T> Page<T> toPage(List<T> items, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findByStudentId(UUID callerId, UUID studentId, Pageable pageable) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        return attendanceRepository.findByEnrollment_StudentId(studentId, pageable).map(this::toResponse);
    }

    @Transactional
    public AttendanceResponse create(UUID callerId, AttendanceCreateRequest request,
                                     String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", request.getEnrollmentId()));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        User recordedBy = userRepository.findById(request.getRecordedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecordedBy()));

        if (!enrollment.getCircle().getId().equals(circle.getId())) {
            throw new BadRequestException("Enrollment does not belong to this circle");
        }
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Enrollment must be ACTIVE to record attendance");
        }

        mosqueAccessService.assertCanMutateStudent(callerId, enrollment.getStudent());

        // Verify caller is assigned to this circle (or has elevated role)
        UserRole role = userRepository.findById(callerId).orElseThrow().getRole();
        if (role != UserRole.SUPER_ADMIN && role != UserRole.MOSQUE_ADMIN) {
            if (!circle.getTeacher().getUser().getId().equals(callerId)) {
                throw new ForbiddenException("Not assigned to this circle");
            }
        }

        Attendance attendance = Attendance.builder()
                .enrollment(enrollment)
                .circle(circle)
                .sessionDate(request.getSessionDate())
                .status(request.getStatus())
                .scheduledStart(request.getScheduledStart())
                .actualCheckIn(request.getActualCheckIn())
                .minutesLate(request.getMinutesLate())
                .parentNotified(request.getParentNotified())
                .absenceReason(request.getAbsenceReason())
                .excuseDocumentUrl(request.getExcuseDocumentUrl())
                .recordedBy(recordedBy)
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    circle.getMosque().getId(),
                    "ATTENDANCE_CREATE",
                    auditReason,
                    "Attendance",
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public AttendanceResponse update(UUID callerId, UUID id, AttendanceUpdateRequest request,
                                     String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        Attendance attendance = findEntityOrThrow(id);
        mosqueAccessService.assertCanMutateStudent(callerId, attendance.getEnrollment().getStudent().getId());

        if (request.getStatus() != null) {
            attendance.setStatus(request.getStatus());
        }
        if (request.getActualCheckIn() != null) {
            attendance.setActualCheckIn(request.getActualCheckIn());
        }
        if (request.getMinutesLate() != null) {
            attendance.setMinutesLate(request.getMinutesLate());
        }
        if (request.getParentNotified() != null) {
            attendance.setParentNotified(request.getParentNotified());
        }
        if (request.getAbsenceReason() != null) {
            attendance.setAbsenceReason(request.getAbsenceReason());
        }
        if (request.getExcuseDocumentUrl() != null) {
            attendance.setExcuseDocumentUrl(request.getExcuseDocumentUrl());
        }

        Attendance saved = attendanceRepository.save(attendance);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    attendance.getCircle().getMosque().getId(),
                    "ATTENDANCE_UPDATE",
                    auditReason,
                    "Attendance",
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public AttendanceResponse submitExcuse(UUID attendanceId, UUID callerId, AttendanceExcuseRequest request) {
        Attendance attendance = findEntityOrThrow(attendanceId);
        UUID studentId = attendance.getEnrollment().getStudent().getId();
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);

        if (request.getAbsenceReason() != null) {
            attendance.setAbsenceReason(parseAbsenceReason(request.getAbsenceReason()));
        }
        if (request.getExcuseDocumentUrl() != null) {
            attendance.setExcuseDocumentUrl(request.getExcuseDocumentUrl());
        }
        attendance.setStatus(AttendanceStatus.EXCUSED);

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public void delete(UUID callerId, UUID id) {
        Attendance attendance = findEntityOrThrow(id);
        mosqueAccessService.assertCanMutateStudent(callerId, attendance.getEnrollment().getStudent().getId());
        attendanceRepository.deleteById(id);
    }

    private Attendance findEntityOrThrow(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
    }

    private AbsenceReason parseAbsenceReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        try {
            return AbsenceReason.valueOf(reason.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown absence reason '{}', falling back to OTHER", reason);
            return AbsenceReason.OTHER;
        }
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .enrollmentId(attendance.getEnrollment().getId())
                .studentName(attendance.getEnrollment().getStudent().getUser().getFullName())
                .circleId(attendance.getCircle().getId())
                .circleName(attendance.getCircle().getName())
                .sessionDate(attendance.getSessionDate())
                .status(attendance.getStatus())
                .scheduledStart(attendance.getScheduledStart())
                .actualCheckIn(attendance.getActualCheckIn())
                .minutesLate(attendance.getMinutesLate())
                .parentNotified(attendance.getParentNotified())
                .absenceReason(attendance.getAbsenceReason())
                .excuseDocumentUrl(attendance.getExcuseDocumentUrl())
                .recordedBy(attendance.getRecordedBy().getId())
                .createdAt(attendance.getCreatedAt())
                .build();
    }
}
