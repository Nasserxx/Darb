package com.darb.services;

import com.darb.dtos.parentstudent.ParentStudentCreateRequest;
import com.darb.dtos.parentstudent.ParentStudentResponse;
import com.darb.dtos.parentstudent.ParentStudentUpdateRequest;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.ParentStudentRepository;
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
public class ParentStudentService {

    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public Page<ParentStudentResponse> findAll(Pageable pageable) {
        return parentStudentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ParentStudentResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public ParentStudentResponse create(ParentStudentCreateRequest request) {
        User parent = userRepository.findById(request.getParentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getParentUserId()));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));

        ParentStudent parentStudent = ParentStudent.builder()
                .parent(parent)
                .student(student)
                .relationship(request.getRelationship())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .receivesNotifications(request.getReceivesNotifications() != null ? request.getReceivesNotifications() : true)
                .build();

        return toResponse(parentStudentRepository.save(parentStudent));
    }

    @Transactional
    public ParentStudentResponse update(UUID id, ParentStudentUpdateRequest request) {
        ParentStudent parentStudent = findEntityOrThrow(id);

        if (request.getRelationship() != null) {
            parentStudent.setRelationship(request.getRelationship());
        }
        if (request.getIsPrimary() != null) {
            parentStudent.setIsPrimary(request.getIsPrimary());
        }
        if (request.getReceivesNotifications() != null) {
            parentStudent.setReceivesNotifications(request.getReceivesNotifications());
        }

        return toResponse(parentStudentRepository.save(parentStudent));
    }

    @Transactional
    public void delete(UUID id) {
        parentStudentRepository.deleteById(id);
    }

    private ParentStudent findEntityOrThrow(UUID id) {
        return parentStudentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentStudent", "id", id));
    }

    private ParentStudentResponse toResponse(ParentStudent parentStudent) {
        return ParentStudentResponse.builder()
                .id(parentStudent.getId())
                .parentUserId(parentStudent.getParent().getId())
                .studentId(parentStudent.getStudent().getId())
                .relationship(parentStudent.getRelationship())
                .isPrimary(parentStudent.getIsPrimary())
                .receivesNotifications(parentStudent.getReceivesNotifications())
                .build();
    }
}
