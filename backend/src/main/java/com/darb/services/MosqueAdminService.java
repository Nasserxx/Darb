package com.darb.services;

import com.darb.dtos.mosqueadmin.MosqueAdminCreateRequest;
import com.darb.dtos.mosqueadmin.MosqueAdminResponse;
import com.darb.dtos.mosqueadmin.MosqueAdminUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.MosqueAdmin;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueAdminRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MosqueAdminService {

    private final MosqueAdminRepository mosqueAdminRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;

    @Transactional(readOnly = true)
    public Page<MosqueAdminResponse> findAll(Pageable pageable) {
        return mosqueAdminRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MosqueAdminResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public MosqueAdminResponse create(MosqueAdminCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        User assignedBy = userRepository.findById(request.getAssignedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedBy()));

        MosqueAdmin mosqueAdmin = MosqueAdmin.builder()
                .user(user)
                .mosque(mosque)
                .permission(request.getPermission())
                .isPrimaryAdmin(request.getIsPrimaryAdmin() != null ? request.getIsPrimaryAdmin() : false)
                .assignedAt(Instant.now())
                .assignedBy(assignedBy)
                .build();

        return toResponse(mosqueAdminRepository.save(mosqueAdmin));
    }

    @Transactional
    public MosqueAdminResponse update(UUID id, MosqueAdminUpdateRequest request) {
        MosqueAdmin mosqueAdmin = findEntityOrThrow(id);

        if (request.getPermission() != null) {
            mosqueAdmin.setPermission(request.getPermission());
        }
        if (request.getIsPrimaryAdmin() != null) {
            mosqueAdmin.setIsPrimaryAdmin(request.getIsPrimaryAdmin());
        }

        return toResponse(mosqueAdminRepository.save(mosqueAdmin));
    }

    @Transactional
    public void delete(UUID id) {
        mosqueAdminRepository.deleteById(id);
    }

    private MosqueAdmin findEntityOrThrow(UUID id) {
        return mosqueAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MosqueAdmin", "id", id));
    }

    private MosqueAdminResponse toResponse(MosqueAdmin mosqueAdmin) {
        return MosqueAdminResponse.builder()
                .id(mosqueAdmin.getId())
                .userId(mosqueAdmin.getUser().getId())
                .mosqueId(mosqueAdmin.getMosque().getId())
                .permission(mosqueAdmin.getPermission())
                .isPrimaryAdmin(mosqueAdmin.getIsPrimaryAdmin())
                .assignedAt(mosqueAdmin.getAssignedAt())
                .assignedBy(mosqueAdmin.getAssignedBy().getId())
                .build();
    }
}
