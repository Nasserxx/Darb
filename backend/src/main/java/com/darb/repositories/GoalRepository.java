package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Goal;
import com.darb.entities.enums.GoalStatus;

import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID>, JpaSpecificationExecutor<Goal> {
    List<Goal> findByStudentId(UUID studentId);
    List<Goal> findByCircleId(UUID circleId);
    List<Goal> findByStatus(GoalStatus status);
    Page<Goal> findByStudentId(UUID studentId, Pageable pageable);
}
