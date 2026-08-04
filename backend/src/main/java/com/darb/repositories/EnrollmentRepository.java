package com.darb.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import com.darb.entities.Enrollment;
import com.darb.entities.enums.EnrollmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {
    List<Enrollment> findByStudentId(UUID studentId);
    Page<Enrollment> findByStudentId(UUID studentId, Pageable pageable);
    List<Enrollment> findByCircleId(UUID circleId);
    List<Enrollment> findByStatus(EnrollmentStatus status);
    boolean existsByStudentIdAndCircleId(UUID studentId, UUID circleId);

    @Lock(LockModeType.OPTIMISTIC)
    Optional<Enrollment> findByStudentIdAndCircleId(UUID studentId, UUID circleId);
    Page<Enrollment> findByCircle_MosqueId(UUID mosqueId, Pageable pageable);
}
