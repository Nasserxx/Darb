package com.darb.services;

import com.darb.dtos.teacher.TeacherCreateRequest;
import com.darb.dtos.teacher.TeacherResponse;
import com.darb.dtos.teacher.TeacherUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Teacher;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.TeacherRepository;
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
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;

    @Transactional(readOnly = true)
    public Page<TeacherResponse> findAll(Pageable pageable) {
        return teacherRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public TeacherResponse create(TeacherCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));

        Teacher teacher = Teacher.builder()
                .user(user)
                .mosque(mosque)
                .specialization(request.getSpecialization())
                .bio(request.getBio())
                .yearsExperience(request.getYearsExperience())
                .ijazahChain(request.getIjazahChain())
                .isAvailable(true)
                .isActive(true)
                .joinedAt(Instant.now())
                .build();

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(UUID id, TeacherUpdateRequest request) {
        Teacher teacher = findEntityOrThrow(id);

        if (request.getSpecialization() != null) {
            teacher.setSpecialization(request.getSpecialization());
        }
        if (request.getBio() != null) {
            teacher.setBio(request.getBio());
        }
        if (request.getYearsExperience() != null) {
            teacher.setYearsExperience(request.getYearsExperience());
        }
        if (request.getIjazahChain() != null) {
            teacher.setIjazahChain(request.getIjazahChain());
        }
        if (request.getIsAvailable() != null) {
            teacher.setIsAvailable(request.getIsAvailable());
        }

        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public void delete(UUID id) {
        Teacher teacher = findEntityOrThrow(id);
        teacher.setIsActive(false);
        teacherRepository.save(teacher);
    }

    private Teacher findEntityOrThrow(UUID id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
    }

    private TeacherResponse toResponse(Teacher teacher) {
        return TeacherResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUser().getId())
                .mosqueId(teacher.getMosque().getId())
                .specialization(teacher.getSpecialization())
                .bio(teacher.getBio())
                .yearsExperience(teacher.getYearsExperience())
                .ijazahChain(teacher.getIjazahChain())
                .isAvailable(teacher.getIsAvailable())
                .isActive(teacher.getIsActive())
                .joinedAt(teacher.getJoinedAt())
                .build();
    }
}
