package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.ParentStudent;

import java.util.List;
import java.util.UUID;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, UUID>, JpaSpecificationExecutor<ParentStudent> {
    List<ParentStudent> findByParentId(UUID parentUserId);
    List<ParentStudent> findByStudentId(UUID studentId);
}
