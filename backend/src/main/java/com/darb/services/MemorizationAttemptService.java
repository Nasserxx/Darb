package com.darb.services;

import com.darb.dtos.memorization.MemorizationAttemptCreateRequest;
import com.darb.dtos.memorization.MemorizationAttemptResponse;
import com.darb.dtos.memorization.MemorizationAttemptUpdateRequest;
import com.darb.dtos.memorization.MemorizationCoverageResponse;
import com.darb.dtos.memorization.StampDto;
import com.darb.dtos.mushaf.HalfPageEntry;
import com.darb.entities.Circle;
import com.darb.entities.Enrollment;
import com.darb.entities.MemorizationAttempt;
import com.darb.entities.Student;
import com.darb.entities.User;
import com.darb.entities.enums.EnrollmentStatus;
import com.darb.entities.enums.PageHalf;
import com.darb.entities.enums.StampType;
import com.darb.entities.enums.UserRole;
import com.darb.exceptions.BadRequestException;
import com.darb.exceptions.ForbiddenException;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.CircleRepository;
import com.darb.repositories.EnrollmentRepository;
import com.darb.repositories.MemorizationAttemptRepository;
import com.darb.repositories.StudentRepository;
import com.darb.repositories.UserRepository;
import com.darb.security.MosqueAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemorizationAttemptService {

    private static final String EDITION = "MADINAH_604";
    private static final int MAX_STAMPS = 50;

    private final MemorizationAttemptRepository memorizationAttemptRepository;
    private final StudentRepository studentRepository;
    private final CircleRepository circleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final MosqueAccessService mosqueAccessService;
    private final MushafMapService mushafMapService;

    @Transactional
    public MemorizationAttemptResponse upsert(UUID callerId, UUID studentId, MemorizationAttemptCreateRequest request) {
        assertStaffCanMutate(callerId, studentId);
        requireActiveEnrollment(studentId, request.getCircleId());

        HalfPageEntry halfMeta = mushafMapService.requireHalf(request.getPage(), request.getHalf());
        validateStamps(halfMeta, request.getStamps());

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Circle circle = circleRepository.findById(request.getCircleId())
                .orElseThrow(() -> new ResourceNotFoundException("Circle", "id", request.getCircleId()));
        User assessor = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId));

        MemorizationAttempt attempt = memorizationAttemptRepository
                .findByStudentIdAndCircleIdAndPageAndHalf(
                        studentId, request.getCircleId(), request.getPage(), request.getHalf())
                .orElse(MemorizationAttempt.builder()
                        .student(student)
                        .circle(circle)
                        .page(request.getPage())
                        .half(request.getHalf())
                        .edition(EDITION)
                        .build());

        attempt.setSessionDate(request.getSessionDate());
        attempt.setAssessor(assessor);
        attempt.setGrade(request.getGrade());
        attempt.setNotes(request.getNotes());
        attempt.setStamps(toEntityStamps(request.getStamps()));

        int tajweed = 0;
        int hifz = 0;
        for (MemorizationAttempt.AttemptStamp stamp : attempt.getStamps()) {
            if (stamp.getType() == StampType.TAJWEED) {
                tajweed++;
            } else if (stamp.getType() == StampType.HIFZ) {
                hifz++;
            }
        }
        attempt.setTajweedCount(tajweed);
        attempt.setHifzCount(hifz);

        return toResponse(memorizationAttemptRepository.save(attempt));
    }

    @Transactional
    public MemorizationAttemptResponse updateNotes(UUID callerId, UUID attemptId, MemorizationAttemptUpdateRequest request) {
        MemorizationAttempt attempt = memorizationAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("MemorizationAttempt", "id", attemptId));
        assertStaffCanMutate(callerId, attempt.getStudent().getId());

        if (request.getGrade() != null) {
            attempt.setGrade(request.getGrade());
        }
        if (request.getNotes() != null) {
            attempt.setNotes(request.getNotes());
        }

        return toResponse(memorizationAttemptRepository.save(attempt));
    }

    @Transactional(readOnly = true)
    public MemorizationAttemptResponse getAttempt(UUID callerId, UUID studentId, short page, PageHalf half, UUID circleId) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        requireActiveEnrollment(studentId, circleId);
        MemorizationAttempt attempt = memorizationAttemptRepository
                .findByStudentIdAndCircleIdAndPageAndHalf(studentId, circleId, page, half)
                .orElseThrow(() -> new ResourceNotFoundException("MemorizationAttempt", "page/half", page + "/" + half));
        return toResponse(attempt);
    }

    @Transactional(readOnly = true)
    public List<MemorizationAttemptResponse> listAttempts(
            UUID callerId, UUID studentId, UUID circleId, Integer juz, Short page) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        requireActiveEnrollment(studentId, circleId);

        List<MemorizationAttempt> attempts;
        if (page != null) {
            attempts = memorizationAttemptRepository.findByStudentIdAndCircleIdAndPageOrderBySessionDateDesc(
                    studentId, circleId, page);
        } else {
            attempts = memorizationAttemptRepository.findByStudentIdAndCircleIdOrderBySessionDateDesc(
                    studentId, circleId);
        }

        if (juz != null) {
            Set<String> halfIdsInJuz = new HashSet<>();
            for (HalfPageEntry halfPage : mushafMapService.getHalvesForJuz(juz)) {
                halfIdsInJuz.add(halfPage.getPage() + "-" + halfPage.getHalf());
            }
            attempts = attempts.stream()
                    .filter(a -> halfIdsInJuz.contains(a.getPage() + "-" + a.getHalf().name()))
                    .toList();
        }

        return attempts.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MemorizationCoverageResponse getCoverage(UUID callerId, UUID studentId, UUID circleId, String grain) {
        mosqueAccessService.assertCanAccessStudent(callerId, studentId);
        requireActiveEnrollment(studentId, circleId);

        String normalizedGrain = grain == null ? "juz" : grain.toLowerCase();
        List<MemorizationAttempt> attempts = memorizationAttemptRepository.findByStudentIdAndCircleId(
                studentId, circleId);
        Set<String> assessedHalfIds = new HashSet<>();
        for (MemorizationAttempt attempt : attempts) {
            assessedHalfIds.add(attempt.getPage() + "-" + attempt.getHalf().name());
        }

        List<MemorizationCoverageResponse.CoverageItem> items = switch (normalizedGrain) {
            case "half" -> buildHalfCoverage(assessedHalfIds);
            case "page" -> buildPageCoverage(assessedHalfIds);
            default -> buildJuzCoverage(assessedHalfIds);
        };

        return MemorizationCoverageResponse.builder()
                .grain(normalizedGrain)
                .studentId(studentId)
                .circleId(circleId)
                .items(items)
                .build();
    }

    private List<MemorizationCoverageResponse.CoverageItem> buildJuzCoverage(Set<String> assessedHalfIds) {
        List<MemorizationCoverageResponse.CoverageItem> items = new ArrayList<>();
        for (int juz = 1; juz <= 30; juz++) {
            List<HalfPageEntry> halves = mushafMapService.getHalvesForJuz(juz);
            int assessed = (int) halves.stream().filter(h -> assessedHalfIds.contains(h.getId())).count();
            items.add(coverageItem(String.valueOf(juz), assessed, halves.size()));
        }
        return items;
    }

    private List<MemorizationCoverageResponse.CoverageItem> buildPageCoverage(Set<String> assessedHalfIds) {
        List<MemorizationCoverageResponse.CoverageItem> items = new ArrayList<>();
        for (int page = 1; page <= 604; page++) {
            List<HalfPageEntry> halves = mushafMapService.getHalvesForPage(page);
            int assessed = (int) halves.stream().filter(h -> assessedHalfIds.contains(h.getId())).count();
            items.add(coverageItem(String.valueOf(page), assessed, halves.size()));
        }
        return items;
    }

    private List<MemorizationCoverageResponse.CoverageItem> buildHalfCoverage(Set<String> assessedHalfIds) {
        List<MemorizationCoverageResponse.CoverageItem> items = new ArrayList<>();
        for (HalfPageEntry half : mushafMapService.getAllHalves()) {
            int assessed = assessedHalfIds.contains(half.getId()) ? 1 : 0;
            items.add(coverageItem(half.getId(), assessed, 1));
        }
        return items;
    }

    private MemorizationCoverageResponse.CoverageItem coverageItem(String key, int assessed, int total) {
        double percent = total == 0 ? 0.0 : (assessed * 100.0) / total;
        return MemorizationCoverageResponse.CoverageItem.builder()
                .key(key)
                .assessed(assessed)
                .total(total)
                .progressPercent(percent)
                .build();
    }

    private void validateStamps(HalfPageEntry halfMeta, List<StampDto> stamps) {
        if (stamps == null) {
            return;
        }
        if (stamps.size() > MAX_STAMPS) {
            throw new BadRequestException("memorization.tooManyStamps");
        }
        for (StampDto stamp : stamps) {
            mushafMapService.validateAyahInHalf(halfMeta, stamp.getSurah(), stamp.getAyah());
        }
    }

    private void requireActiveEnrollment(UUID studentId, UUID circleId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCircleId(studentId, circleId)
                .orElseThrow(() -> new BadRequestException("memorization.noEnrollment"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("memorization.noEnrollment");
        }
    }

    private void assertStaffCanMutate(UUID callerId, UUID studentId) {
        UserRole role = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", callerId))
                .getRole();
        if (role == UserRole.STUDENT) {
            throw new ForbiddenException("memorization.assessment.staffOnly");
        }
        mosqueAccessService.assertCanMutateStudent(callerId, studentId);
    }

    private List<MemorizationAttempt.AttemptStamp> toEntityStamps(List<StampDto> stamps) {
        if (stamps == null || stamps.isEmpty()) {
            return new ArrayList<>();
        }
        List<MemorizationAttempt.AttemptStamp> entityStamps = new ArrayList<>();
        for (StampDto stamp : stamps) {
            MemorizationAttempt.AttemptStamp entityStamp = new MemorizationAttempt.AttemptStamp();
            entityStamp.setType(stamp.getType());
            entityStamp.setSurah(stamp.getSurah());
            entityStamp.setAyah(stamp.getAyah());
            entityStamp.setX(stamp.getX());
            entityStamp.setY(stamp.getY());
            entityStamps.add(entityStamp);
        }
        return entityStamps;
    }

    private List<StampDto> toDtoStamps(List<MemorizationAttempt.AttemptStamp> stamps) {
        if (stamps == null) {
            return List.of();
        }
        return stamps.stream()
                .map(s -> StampDto.builder()
                        .type(s.getType())
                        .surah(s.getSurah())
                        .ayah(s.getAyah())
                        .x(s.getX())
                        .y(s.getY())
                        .build())
                .toList();
    }

    private MemorizationAttemptResponse toResponse(MemorizationAttempt attempt) {
        return MemorizationAttemptResponse.builder()
                .id(attempt.getId())
                .studentId(attempt.getStudent().getId())
                .circleId(attempt.getCircle().getId())
                .page(attempt.getPage())
                .half(attempt.getHalf())
                .edition(attempt.getEdition())
                .sessionDate(attempt.getSessionDate())
                .assessorId(attempt.getAssessor().getId())
                .grade(attempt.getGrade())
                .notes(attempt.getNotes())
                .stamps(toDtoStamps(attempt.getStamps()))
                .tajweedCount(attempt.getTajweedCount())
                .hifzCount(attempt.getHifzCount())
                .createdAt(attempt.getCreatedAt())
                .updatedAt(attempt.getUpdatedAt())
                .build();
    }
}
