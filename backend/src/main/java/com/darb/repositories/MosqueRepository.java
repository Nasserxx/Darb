package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.darb.entities.Mosque;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MosqueRepository extends JpaRepository<Mosque, UUID>, JpaSpecificationExecutor<Mosque> {
    List<Mosque> findByIsActiveTrue();
    List<Mosque> findByCity(String city);

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

    @Query(
            value = """
                    SELECT * FROM mosques
                    WHERE is_active = true
                      AND (:q IS NULL OR :q = '' OR lower(name) LIKE lower(concat('%', :q, '%')))
                      AND (:city IS NULL OR :city = '' OR lower(city) = lower(:city))
                    ORDER BY name
                    LIMIT 20
                    """,
            nativeQuery = true)
    List<Mosque> searchActive(@Param("q") String q, @Param("city") String city);
}
