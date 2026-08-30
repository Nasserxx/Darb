package com.darb.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.darb.entities.Circle;
import com.darb.entities.enums.CircleStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CircleRepository extends JpaRepository<Circle, UUID>, JpaSpecificationExecutor<Circle> {
    List<Circle> findByMosqueId(UUID mosqueId);
    Page<Circle> findByMosqueId(UUID mosqueId, Pageable pageable);
    List<Circle> findByTeacherId(UUID teacherId);
    Page<Circle> findByTeacherId(UUID teacherId, Pageable pageable);
    List<Circle> findByStatus(CircleStatus status);
    List<Circle> findByMosqueIdAndStatus(UUID mosqueId, CircleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Circle c WHERE c.id = :id")
    Optional<Circle> findByIdForUpdate(@Param("id") UUID id);
}
