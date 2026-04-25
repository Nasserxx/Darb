package com.darb.services;

import com.darb.dtos.attendance.AttendanceCreateRequest;
import com.darb.dtos.attendance.AttendanceResponse;
import com.darb.dtos.attendance.AttendanceUpdateRequest;
import com.darb.entities.Attendance;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.AttendanceRepository;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
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
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CircleRepository circleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findAll(Pageable pageable) {
        return attendanceRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findByCircleId(UUID circleId, Pageable pageable) {
        return attendanceRepository.findByCircleId(circleId, pageable).map(this::toResponse);
    }

    @Transactional
    public AttendanceResponse create(AttendanceCreateRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", request.getEnrollmentId()));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        User recordedBy = userRepository.findById(request.getRecordedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getRecordedBy()));

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

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceResponse update(UUID id, AttendanceUpdateRequest request) {
        Attendance attendance = findEntityOrThrow(id);

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

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public void delete(UUID id) {
        attendanceRepository.deleteById(id);
    }

    private Attendance findEntityOrThrow(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .enrollmentId(attendance.getEnrollment().getId())
                .circleId(attendance.getCircle().getId())
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
