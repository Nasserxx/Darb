package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Circle;
import com.darb.entities.enums.CircleStatus;

import java.util.List;
import java.util.UUID;

public interface CircleRepository extends JpaRepository<Circle, UUID>, JpaSpecificationExecutor<Circle> {
    List<Circle> findByMosqueId(UUID mosqueId);
    List<Circle> findByTeacherId(UUID teacherId);
    List<Circle> findByStatus(CircleStatus status);
    List<Circle> findByMosqueIdAndStatus(UUID mosqueId, CircleStatus status);
}
