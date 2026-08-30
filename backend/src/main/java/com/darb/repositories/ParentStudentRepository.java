package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.ParentStudent;

import java.util.List;
import java.util.UUID;

public interface ParentStudentRepository extends JpaRepository<ParentStudent, UUID>, JpaSpecificationExecutor<ParentStudent> {
    List<ParentStudent> findByParentId(UUID parentUserId);
    List<ParentStudent> findByStudentId(UUID studentId);
    Page<ParentStudent> findByMosqueId(UUID mosqueId, Pageable pageable);

    boolean existsByParent_IdAndStudent_Id(UUID parentId, UUID studentId);

    boolean existsByParent_IdAndStudent_IdAndIdNot(UUID parentId, UUID studentId, UUID id);

    boolean existsByParent_IdAndStudent_Mosque_Id(UUID parentId, UUID mosqueId);
}
