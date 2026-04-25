package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Teacher;

import java.util.List;
import java.util.UUID;

public interface TeacherRepository extends JpaRepository<Teacher, UUID>, JpaSpecificationExecutor<Teacher> {
    List<Teacher> findByUserId(UUID userId);
    List<Teacher> findByMosqueId(UUID mosqueId);
    List<Teacher> findByIsActiveTrue();
    List<Teacher> findByIsAvailableTrueAndIsActiveTrue();
}
