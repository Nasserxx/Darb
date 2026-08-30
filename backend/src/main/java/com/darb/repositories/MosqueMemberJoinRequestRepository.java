package com.darb.repositories;

import com.darb.entities.MosqueMemberJoinRequest;
import com.darb.entities.enums.JoinRequestDirection;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.entities.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MosqueMemberJoinRequestRepository extends JpaRepository<MosqueMemberJoinRequest, UUID> {

    boolean existsByUserIdAndMosqueIdAndRequestedRoleAndStatus(
            UUID userId,
            UUID mosqueId,
            UserRole requestedRole,
            JoinRequestStatus status);

    List<MosqueMemberJoinRequest> findByUserIdAndDirectionAndStatus(
            UUID userId,
            JoinRequestDirection direction,
            JoinRequestStatus status);

    List<MosqueMemberJoinRequest> findByMosqueIdAndStatus(UUID mosqueId, JoinRequestStatus status);

    List<MosqueMemberJoinRequest> findByUserIdAndStatusAndRequestedRole(
            UUID userId,
            JoinRequestStatus status,
            UserRole requestedRole);

    List<MosqueMemberJoinRequest> findByStatus(JoinRequestStatus status);
}
