package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.MemorizationProgress;

import java.util.List;
import java.util.UUID;

public interface MemorizationProgressRepository extends JpaRepository<MemorizationProgress, UUID>, JpaSpecificationExecutor<MemorizationProgress> {
    List<MemorizationProgress> findByStudentId(UUID studentId);
    List<MemorizationProgress> findByCircleId(UUID circleId);
    List<MemorizationProgress> findByTeacherId(UUID teacherId);
    List<MemorizationProgress> findByStudentIdAndCircleId(UUID studentId, UUID circleId);
    Page<MemorizationProgress> findByStudentId(UUID studentId, Pageable pageable);
    Page<MemorizationProgress> findByCircleId(UUID circleId, Pageable pageable);
}
