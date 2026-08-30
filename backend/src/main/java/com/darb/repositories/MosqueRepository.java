package com.darb.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.darb.entities.Mosque;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MosqueRepository extends JpaRepository<Mosque, UUID>, JpaSpecificationExecutor<Mosque> {
    Page<Mosque> findByIsActiveTrueAndIdIn(Collection<UUID> ids, Pageable pageable);

    @Query("""
            SELECT DISTINCT m.city FROM Mosque m
            WHERE UPPER(m.addressCountry) = :country
              AND m.city IS NOT NULL AND TRIM(m.city) <> ''
              AND (:activeOnly = false OR m.isActive = true)
            ORDER BY m.city ASC
            """)
    List<String> findDistinctCitiesByCountry(
            @Param("country") String country,
            @Param("activeOnly") boolean activeOnly);

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
