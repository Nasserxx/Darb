package com.darb.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.darb.entities.User;
import com.darb.entities.enums.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
    List<User> findByRole(UserRole role);
    List<User> findByIsActiveTrue();

    @Query("""
            SELECT DISTINCT u.addressState FROM User u
            WHERE UPPER(u.addressCountry) = :country
              AND u.addressState IS NOT NULL AND TRIM(u.addressState) <> ''
            ORDER BY u.addressState ASC
            """)
    List<String> findDistinctStatesByCountry(@Param("country") String country);

    @Query("""
            SELECT DISTINCT u.city FROM User u
            WHERE UPPER(u.addressCountry) = :country
              AND u.city IS NOT NULL AND TRIM(u.city) <> ''
            ORDER BY u.city ASC
            """)
    List<String> findDistinctCitiesByCountry(@Param("country") String country);

    @Query("""
            SELECT DISTINCT u.city FROM User u
            WHERE UPPER(u.addressCountry) = :country
              AND u.city IS NOT NULL AND TRIM(u.city) <> ''
              AND LOWER(TRIM(u.addressState)) = LOWER(TRIM(:state))
            ORDER BY u.city ASC
            """)
    List<String> findDistinctCitiesByCountryAndState(
            @Param("country") String country,
            @Param("state") String state);
}
