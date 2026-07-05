package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Teacher;

import java.util.List;
import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID>, JpaSpecificationExecutor<Teacher> {
    List<Teacher> findByUserId(UUID userId);
    List<Teacher> findByMosqueId(UUID mosqueId);
    Page<Teacher> findByMosqueId(UUID mosqueId, Pageable pageable);
    List<Teacher> findByIsActiveTrue();
    List<Teacher> findByIsAvailableTrueAndIsActiveTrue();
}
