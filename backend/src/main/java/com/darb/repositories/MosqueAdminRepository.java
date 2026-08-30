package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.MosqueAdmin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MosqueAdminRepository extends JpaRepository<MosqueAdmin, UUID>, JpaSpecificationExecutor<MosqueAdmin> {
    boolean existsByUserId(UUID userId);
    List<MosqueAdmin> findByUserId(UUID userId);
    List<MosqueAdmin> findByMosqueId(UUID mosqueId);
    List<MosqueAdmin> findByMosqueIdAndIsActiveTrue(UUID mosqueId);
    Page<MosqueAdmin> findByMosqueId(UUID mosqueId, Pageable pageable);
    Optional<MosqueAdmin> findByUserIdAndMosqueId(UUID userId, UUID mosqueId);
    boolean existsByUserIdAndMosqueId(UUID userId, UUID mosqueId);
    boolean existsByUserIdAndMosqueIdAndIsActiveTrue(UUID userId, UUID mosqueId);
    List<MosqueAdmin> findByUserIdAndIsActiveTrue(UUID userId);
}
