package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Student;
import com.darb.entities.enums.EnrollmentStatus;

import java.util.List;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {
    List<Student> findByUserId(UUID userId);
    List<Student> findByMosqueId(UUID mosqueId);
    Page<Student> findByMosqueId(UUID mosqueId, Pageable pageable);
    List<Student> findByStatus(EnrollmentStatus status);
    List<Student> findByStatusNot(EnrollmentStatus status);
}
