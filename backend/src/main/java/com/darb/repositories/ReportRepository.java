package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.darb.entities.Report;
import com.darb.entities.enums.ReportType;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {
    List<Report> findByMosqueId(UUID mosqueId);
    List<Report> findByGeneratedById(UUID generatedBy);
    List<Report> findByType(ReportType type);
    Page<Report> findByMosqueId(UUID mosqueId, Pageable pageable);
}
