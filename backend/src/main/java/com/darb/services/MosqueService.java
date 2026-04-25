package com.darb.services;

import com.darb.dtos.mosque.MosqueCreateRequest;
import com.darb.dtos.mosque.MosqueResponse;
import com.darb.dtos.mosque.MosqueUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MosqueService {

    private final MosqueRepository mosqueRepository;

    @Transactional(readOnly = true)
    public Page<MosqueResponse> findAll(Pageable pageable) {
        return mosqueRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MosqueResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public MosqueResponse create(MosqueCreateRequest request) {
        Mosque mosque = Mosque.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .timezone(request.getTimezone())
                .isActive(true)
                .build();

        return toResponse(mosqueRepository.save(mosque));
    }

    @Transactional
    public MosqueResponse update(UUID id, MosqueUpdateRequest request) {
        Mosque mosque = findEntityOrThrow(id);

        if (request.getName() != null) {
            mosque.setName(request.getName());
        }
        if (request.getAddress() != null) {
            mosque.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            mosque.setCity(request.getCity());
        }
        if (request.getPhone() != null) {
            mosque.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            mosque.setEmail(request.getEmail());
        }
        if (request.getLogoUrl() != null) {
            mosque.setLogoUrl(request.getLogoUrl());
        }
        if (request.getTimezone() != null) {
            mosque.setTimezone(request.getTimezone());
        }
        if (request.getSettings() != null) {
            mosque.setSettings(request.getSettings());
        }

        return toResponse(mosqueRepository.save(mosque));
    }

    @Transactional
    public void delete(UUID id) {
        Mosque mosque = findEntityOrThrow(id);
        mosque.setIsActive(false);
        mosqueRepository.save(mosque);
    }

    private Mosque findEntityOrThrow(UUID id) {
        return mosqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", id));
    }

    private MosqueResponse toResponse(Mosque mosque) {
        return MosqueResponse.builder()
                .id(mosque.getId())
                .name(mosque.getName())
                .address(mosque.getAddress())
                .city(mosque.getCity())
                .phone(mosque.getPhone())
                .email(mosque.getEmail())
                .logoUrl(mosque.getLogoUrl())
                .timezone(mosque.getTimezone())
                .settings(mosque.getSettings())
                .isActive(mosque.getIsActive())
                .createdAt(mosque.getCreatedAt())
                .build();
    }
}
