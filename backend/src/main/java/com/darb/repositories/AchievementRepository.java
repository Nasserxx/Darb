package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Achievement;
import com.darb.entities.enums.AchievementType;

import java.util.List;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID>, JpaSpecificationExecutor<Achievement> {
    List<Achievement> findByStudentId(UUID studentId);
    List<Achievement> findByMosqueId(UUID mosqueId);
    List<Achievement> findByType(AchievementType type);
    Page<Achievement> findByStudentId(UUID studentId, Pageable pageable);
    Page<Achievement> findByMosqueId(UUID mosqueId, Pageable pageable);
}
