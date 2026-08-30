package com.darb.repositories;

import com.darb.entities.LessonAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LessonAssignmentRepository extends JpaRepository<LessonAssignment, UUID> {

    Optional<LessonAssignment> findByStudentIdAndCircleId(UUID studentId, UUID circleId);
}
