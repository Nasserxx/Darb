package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Enrollment;
import com.darb.entities.enums.EnrollmentStatus;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {
    List<Enrollment> findByStudentId(UUID studentId);
    List<Enrollment> findByCircleId(UUID circleId);
    List<Enrollment> findByStatus(EnrollmentStatus status);
    boolean existsByStudentIdAndCircleId(UUID studentId, UUID circleId);
}
