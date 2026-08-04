package com.darb.repositories;

import com.darb.entities.OverrideAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OverrideAuditLogRepository extends JpaRepository<OverrideAuditLog, UUID> {
}
