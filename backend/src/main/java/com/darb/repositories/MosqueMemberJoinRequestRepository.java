package com.darb.repositories;

import com.darb.entities.MosqueMemberJoinRequest;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MosqueMemberJoinRequestRepository extends JpaRepository<MosqueMemberJoinRequest, UUID> {

    Optional<MosqueMemberJoinRequest> findByUserIdAndStatus(UUID userId, JoinRequestStatus status);

    boolean existsByUserIdAndStatus(UUID userId, JoinRequestStatus status);

    List<MosqueMemberJoinRequest> findByMosqueIdAndStatus(UUID mosqueId, JoinRequestStatus status);

    Optional<MosqueMemberJoinRequest> findByUserIdAndStatusAndRequestedRole(
            UUID userId,
            JoinRequestStatus status,
            UserRole requestedRole);
}
