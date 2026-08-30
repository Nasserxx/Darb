package com.darb.services;

import com.darb.dtos.mosque.MemberJoinRequestResponse;
import com.darb.dtos.parentstudent.ParentStudentCreateRequest;
import com.darb.dtos.parentstudent.ParentStudentJoinPreviewResponse;
import com.darb.dtos.parentstudent.ParentStudentResponse;
import com.darb.dtos.parentstudent.ParentStudentUpdateRequest;
import com.darb.dtos.student.StudentResponse;
import com.darb.entities.ParentStudent;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.ParentRelationship;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.ParentStudentRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import com.darb.util.NameFilterSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentStudentService {

    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final MosqueAccessService mosqueAccessService;
    private final OverrideAuditService overrideAuditService;
    private final ObjectProvider<MosqueMemberJoinRequestService> joinRequestService;

    @Transactional(readOnly = true)
    public Page<ParentStudentResponse> findAll(UUID callerId, Pageable pageable, UUID mosqueIdFilter, String q) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId));
        if (caller.getRole() == UserRole.STUDENT) {
            List<Student> mine = studentRepository.findByUserId(callerId);
            if (mine.isEmpty()) {
                return Page.empty(pageable);
            }
            UUID studentId = mine.getFirst().getId();
            Specification<ParentStudent> ownSpec = (root, query, cb) ->
                    cb.equal(root.get("student").get("id"), studentId);
            ownSpec = NameFilterSpecs.and(ownSpec, NameFilterSpecs.parentUserFullNameLike(q));
            return parentStudentRepository.findAll(ownSpec, pageable).map(this::toResponse);
        }

        mosqueAccessService.assertValidNameFilter(callerId, mosqueIdFilter, q);
        if (mosqueAccessService.shouldReturnEmptyListPage(callerId)) {
            return Page.empty(pageable);
        }
        UUID mosqueId = mosqueAccessService.resolveEffectiveMosqueIdForList(callerId, mosqueIdFilter);
        Specification<ParentStudent> spec = (root, query, cb) -> cb.conjunction();
        if (mosqueId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("student").get("mosque").get("id"), mosqueId));
        }
        spec = NameFilterSpecs.and(spec, NameFilterSpecs.parentUserFullNameLike(q));
        return parentStudentRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ParentStudentResponse findById(UUID callerId, UUID id) {
        ParentStudent link = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessParentStudent(callerId, link);
        return toResponse(link);
    }

    @Transactional
    public MemberJoinRequestResponse create(UUID callerId, ParentStudentCreateRequest request,
                                        String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);

        User parent = userRepository.findById(request.getParentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getParentUserId()));
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.getStudentId()));

        mosqueAccessService.assertCanAccessStudent(callerId, student);

        MemberJoinRequestResponse saved = joinRequestService.getObject().invite(
                callerId,
                parent.getId(),
                student.getMosque().getId(),
                UserRole.PARENT,
                student.getId(),
                request.getRelationship());
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    student.getMosque().getId(),
                    "PARENT_STUDENT_CREATE",
                    auditReason,
                    "MosqueJoinRequest",
                    saved.getId());
        }
        return saved;
    }

    @Transactional
    public ParentStudentResponse update(UUID callerId, UUID id, ParentStudentUpdateRequest request,
                                        String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        ParentStudent parentStudent = findEntityOrThrow(id);
        mosqueAccessService.assertCanAccessParentStudent(callerId, parentStudent);

        UUID effectiveParentId = request.getParentUserId() != null
                ? request.getParentUserId()
                : parentStudent.getParent().getId();
        UUID effectiveStudentId = request.getStudentId() != null
                ? request.getStudentId()
                : parentStudent.getStudent().getId();

        boolean pairChanged = !effectiveParentId.equals(parentStudent.getParent().getId())
                || !effectiveStudentId.equals(parentStudent.getStudent().getId());

        if (pairChanged) {
            User parent = userRepository.findById(effectiveParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", effectiveParentId));
            Student student = studentRepository.findById(effectiveStudentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", effectiveStudentId));

            mosqueAccessService.assertCanAccessStudent(callerId, student);

            if (parentStudentRepository.existsByParent_IdAndStudent_IdAndIdNot(
                    parent.getId(), student.getId(), parentStudent.getId())) {
                throw new BadRequestException("This parent is already linked to this student");
            }

            parentStudent.setParent(parent);
            parentStudent.setStudent(student);
            parentStudent.setMosque(student.getMosque());
        }

        if (request.getRelationship() != null) {
            parentStudent.setRelationship(request.getRelationship());
        }
        if (request.getIsPrimary() != null) {
            parentStudent.setIsPrimary(request.getIsPrimary());
        }
        if (request.getReceivesNotifications() != null) {
            parentStudent.setReceivesNotifications(request.getReceivesNotifications());
        }

        ParentStudent saved = parentStudentRepository.save(parentStudent);
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    parentStudent.getStudent().getMosque().getId(),
                    "PARENT_STUDENT_UPDATE",
                    auditReason,
                    "ParentStudent",
                    saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID callerId, UUID id, String auditReasonHeader, String auditReasonBody) {
        String auditReason = overrideAuditService.gateSuperAdmin(callerId, auditReasonHeader, auditReasonBody);
        ParentStudent parentStudent = findEntityOrThrow(id);
        UUID mosqueId = parentStudent.getStudent().getMosque().getId();
        if (auditReason != null) {
            overrideAuditService.record(
                    callerId,
                    mosqueId,
                    "PARENT_STUDENT_DELETE",
                    auditReason,
                    "ParentStudent",
                    id);
        }
        parentStudentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getMyChildren(UUID parentUserId) {
        return parentStudentRepository.findByParentId(parentUserId).stream()
                .map(ps -> toStudentResponse(ps.getStudent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ParentStudentJoinPreviewResponse previewJoinByInviteCode(String inviteCode) {
        String normalizedCode = normalizeInviteCode(inviteCode);
        Student student = studentRepository.findByParentInviteCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "parentInviteCode", normalizedCode));

        return ParentStudentJoinPreviewResponse.builder()
                .mosqueName(student.getMosque().getName())
                .studentName(student.getUser().getFullName())
                .build();
    }

    @Transactional
    public ParentStudentResponse joinByInviteCode(UUID parentUserId, String inviteCode) {
        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", parentUserId));

        if (parent.getRole() != UserRole.PARENT) {
            throw new ForbiddenException("Only parents can link using this endpoint");
        }

        String normalizedCode = normalizeInviteCode(inviteCode);
        Student student = studentRepository.findByParentInviteCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "parentInviteCode", normalizedCode));

        // Check if already linked
        boolean alreadyLinked = parentStudentRepository.findByParentId(parentUserId).stream()
                .anyMatch(ps -> ps.getStudent().getId().equals(student.getId()));
        if (alreadyLinked) {
            throw new BadRequestException("You are already linked to this student");
        }

        ParentStudent parentStudent = ParentStudent.builder()
                .parent(parent)
                .student(student)
                .mosque(student.getMosque())
                .relationship(ParentRelationship.PARENT)
                .isPrimary(true)
                .receivesNotifications(true)
                .build();

        return toResponse(parentStudentRepository.save(parentStudent));
    }

    private ParentStudent findEntityOrThrow(UUID id) {
        return parentStudentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentStudent", "id", id));
    }

    private String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new BadRequestException("Invite code is required");
        }
        return inviteCode.trim();
    }

    private ParentStudentResponse toResponse(ParentStudent parentStudent) {
        return ParentStudentResponse.builder()
                .id(parentStudent.getId())
                .parentUserId(parentStudent.getParent().getId())
                .parentName(parentStudent.getParent().getFullName())
                .studentId(parentStudent.getStudent().getId())
                .studentName(parentStudent.getStudent().getUser().getFullName())
                .relationship(parentStudent.getRelationship())
                .isPrimary(parentStudent.getIsPrimary())
                .receivesNotifications(parentStudent.getReceivesNotifications())
                .build();
    }

    private StudentResponse toStudentResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .userId(student.getUser().getId())
                .fullName(student.getUser().getFullName())
                .mosqueId(student.getMosque().getId())
                .mosqueName(student.getMosque().getName())
                .medicalNotes(student.getMedicalNotes())
                .memorizedJuz(student.getMemorizedJuz())
                .totalAbsences(student.getTotalAbsences())
                .totalLateArrivals(student.getTotalLateArrivals())
                .status(student.getStatus())
                .enrolledAt(student.getEnrolledAt())
                .build();
    }
}
