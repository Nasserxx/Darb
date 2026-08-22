package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.darb.entities.Mosque;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MosqueRepository extends JpaRepository<Mosque, UUID>, JpaSpecificationExecutor<Mosque> {
    Page<Mosque> findByIsActiveTrueAndIdIn(Collection<UUID> ids, Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM mosques
                    WHERE is_active = true
                      AND settings->>'adminInviteCode' = :inviteCode
                    LIMIT 1
                    """,
            nativeQuery = true)
    Optional<Mosque> findActiveByAdminInviteCode(@Param("inviteCode") String inviteCode);

    @Query(
            value = """
                    SELECT * FROM mosques
                    WHERE is_active = true
                      AND settings->>'teacherInviteCode' = :inviteCode
                    LIMIT 1
                    """,
            nativeQuery = true)
    Optional<Mosque> findActiveByTeacherInviteCode(@Param("inviteCode") String inviteCode);

    @Query(
            value = """
                    SELECT * FROM mosques
                    WHERE is_active = true
                      AND settings->>'studentInviteCode' = :inviteCode
                    LIMIT 1
                    """,
            nativeQuery = true)
    Optional<Mosque> findActiveByStudentInviteCode(@Param("inviteCode") String inviteCode);
}
