package com.darb.services;

import com.darb.dtos.stuckwork.StuckWorkItem;
import com.darb.dtos.stuckwork.StuckWorkItemKind;
import com.darb.entities.MosqueMemberJoinRequest;
import com.darb.entities.enums.JoinRequestStatus;
import com.darb.repositories.MosqueMemberJoinRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StuckWorkService {

    private final MosqueMemberJoinRequestRepository joinRequestRepository;

    @Transactional(readOnly = true)
    public List<StuckWorkItem> listFleetStuckWork() {
        List<MosqueMemberJoinRequest> pending =
                joinRequestRepository.findByStatus(JoinRequestStatus.PENDING);

        Map<UUID, Long> pendingCountByMosque = pending.stream()
                .collect(Collectors.groupingBy(
                        request -> request.getMosque().getId(),
                        Collectors.counting()));

        return pending.stream()
                .map(request -> toPendingJoinItem(
                        request,
                        pendingCountByMosque.getOrDefault(request.getMosque().getId(), 0L).intValue()))
                .sorted(Comparator
                        .comparingInt(StuckWorkItem::getDensityScore).reversed()
                        .thenComparing(StuckWorkItem::getCreatedAt))
                .toList();
    }

    private StuckWorkItem toPendingJoinItem(MosqueMemberJoinRequest request, int densityScore) {
        String userName = request.getUser().getFullName();
        String roleLabel = request.getRequestedRole().name().toLowerCase().replace('_', ' ');
        return StuckWorkItem.builder()
                .kind(StuckWorkItemKind.PENDING_JOIN)
                .mosqueId(request.getMosque().getId())
                .mosqueName(request.getMosque().getName())
                .resourceId(request.getId())
                .summary("%s requested to join as %s".formatted(userName, roleLabel))
                .createdAt(request.getCreatedAt())
                .densityScore(densityScore)
                .build();
    }
}
