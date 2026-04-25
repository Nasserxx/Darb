package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Mosque;

import java.util.List;
import java.util.UUID;

public interface MosqueRepository extends JpaRepository<Mosque, UUID>, JpaSpecificationExecutor<Mosque> {
    List<Mosque> findByIsActiveTrue();
    List<Mosque> findByCity(String city);
}
