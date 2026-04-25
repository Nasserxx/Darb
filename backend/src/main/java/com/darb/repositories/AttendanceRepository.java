package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Attendance;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID>, JpaSpecificationExecutor<Attendance> {
    List<Attendance> findByEnrollmentId(UUID enrollmentId);
    List<Attendance> findByCircleId(UUID circleId);
    List<Attendance> findByCircleIdAndSessionDate(UUID circleId, LocalDate sessionDate);
    List<Attendance> findBySessionDate(LocalDate sessionDate);
    Page<Attendance> findByCircleId(UUID circleId, Pageable pageable);
}
