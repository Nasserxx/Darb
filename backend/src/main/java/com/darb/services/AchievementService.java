package com.darb.services;

import com.darb.dtos.achievement.AchievementCreateRequest;
import com.darb.dtos.achievement.AchievementResponse;
import com.darb.dtos.achievement.AchievementUpdateRequest;
import com.darb.entities.Achievement;
import com.darb.entities.Mosque;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.AchievementRepository;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
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
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final StudentRepository studentRepository;
    private final MosqueRepository mosqueRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AchievementResponse> findAll(Pageable pageable) {
        return achievementRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AchievementResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<AchievementResponse> findByStudentId(UUID studentId, Pageable pageable) {
        return achievementRepository.findByStudentId(studentId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AchievementResponse> findByMosqueId(UUID mosqueId, Pageable pageable) {
        return achievementRepository.findByMosqueId(mosqueId, pageable).map(this::toResponse);
    }

    @Transactional
    public AchievementResponse create(AchievementCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        User awardedBy = userRepository.findById(request.getAwardedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAwardedBy()));

        Achievement achievement = Achievement.builder()
                .student(student)
                .mosque(mosque)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .badgeUrl(request.getBadgeUrl())
                .awardedBy(awardedBy)
                .awardedDate(request.getAwardedDate())
                .build();

        return toResponse(achievementRepository.save(achievement));
    }

    @Transactional
    public AchievementResponse update(UUID id, AchievementUpdateRequest request) {
        Achievement achievement = findEntityOrThrow(id);

        if (request.getTitle() != null) {
            achievement.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            achievement.setDescription(request.getDescription());
        }
        if (request.getBadgeUrl() != null) {
            achievement.setBadgeUrl(request.getBadgeUrl());
        }

        return toResponse(achievementRepository.save(achievement));
    }

    @Transactional
    public void delete(UUID id) {
        achievementRepository.deleteById(id);
    }

    private Achievement findEntityOrThrow(UUID id) {
        return achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement", "id", id));
    }

    private AchievementResponse toResponse(Achievement achievement) {
        return AchievementResponse.builder()
                .id(achievement.getId())
                .studentId(achievement.getStudent().getId())
                .mosqueId(achievement.getMosque().getId())
                .type(achievement.getType())
                .title(achievement.getTitle())
                .description(achievement.getDescription())
                .badgeUrl(achievement.getBadgeUrl())
                .awardedBy(achievement.getAwardedBy().getId())
                .awardedDate(achievement.getAwardedDate())
                .createdAt(achievement.getCreatedAt())
                .build();
    }
}
