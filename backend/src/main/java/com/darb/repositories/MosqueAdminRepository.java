package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.MosqueAdmin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MosqueAdminRepository extends JpaRepository<MosqueAdmin, UUID>, JpaSpecificationExecutor<MosqueAdmin> {
    List<MosqueAdmin> findByUserId(UUID userId);
    List<MosqueAdmin> findByMosqueId(UUID mosqueId);
    Optional<MosqueAdmin> findByUserIdAndMosqueId(UUID userId, UUID mosqueId);
    boolean existsByUserIdAndMosqueId(UUID userId, UUID mosqueId);
}
