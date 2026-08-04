package com.darb.repositories;

import com.darb.entities.RefreshTokenHash;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenHashRepository extends JpaRepository<RefreshTokenHash, UUID> {

    Optional<RefreshTokenHash> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);
}
