package com.darb.services;

import com.darb.dtos.student.StudentCreateRequest;
import com.darb.dtos.student.StudentResponse;
import com.darb.dtos.student.StudentUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.StudentRepository;
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
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MosqueRepository mosqueRepository;

    @Transactional(readOnly = true)
    public Page<StudentResponse> findAll(Pageable pageable) {
        return studentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));

        Student student = Student.builder()
                .user(user)
                .mosque(mosque)
                .nationalId(request.getNationalId())
                .medicalNotes(request.getMedicalNotes())
                .memorizedJuz(request.getMemorizedJuz())
                .totalAbsences(0)
                .totalLateArrivals(0)
                .status(EnrollmentStatus.ACTIVE)
                .enrolledAt(Instant.now())
                .build();

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse update(UUID id, StudentUpdateRequest request) {
        Student student = findEntityOrThrow(id);

        if (request.getNationalId() != null) {
            student.setNationalId(request.getNationalId());
        }
        if (request.getMedicalNotes() != null) {
            student.setMedicalNotes(request.getMedicalNotes());
        }
        if (request.getMemorizedJuz() != null) {
            student.setMemorizedJuz(request.getMemorizedJuz());
        }
        if (request.getTotalAbsences() != null) {
            student.setTotalAbsences(request.getTotalAbsences());
        }
        if (request.getTotalLateArrivals() != null) {
            student.setTotalLateArrivals(request.getTotalLateArrivals());
        }

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void delete(UUID id) {
        Student student = findEntityOrThrow(id);
        student.setStatus(EnrollmentStatus.WITHDRAWN);
        studentRepository.save(student);
    }

    private Student findEntityOrThrow(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }

    private StudentResponse toResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .userId(student.getUser().getId())
                .mosqueId(student.getMosque().getId())
                .nationalId(student.getNationalId())
                .medicalNotes(student.getMedicalNotes())
                .memorizedJuz(student.getMemorizedJuz())
                .totalAbsences(student.getTotalAbsences())
                .totalLateArrivals(student.getTotalLateArrivals())
                .status(student.getStatus())
                .enrolledAt(student.getEnrolledAt())
                .build();
    }
}
