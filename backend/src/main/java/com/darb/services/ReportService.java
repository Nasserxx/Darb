package com.darb.services;

import com.darb.dtos.report.ReportCreateRequest;
import com.darb.dtos.report.ReportResponse;
import com.darb.dtos.report.ReportUpdateRequest;
import com.darb.entities.Mosque;
import com.darb.entities.Report;
import com.darb.entities.User;
import com.darb.exceptions.ResourceNotFoundException;
import com.darb.repositories.MosqueRepository;
import com.darb.repositories.ReportRepository;
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
public class ReportService {

    private final ReportRepository reportRepository;
    private final MosqueRepository mosqueRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ReportResponse> findAll(Pageable pageable) {
        return reportRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReportResponse findById(UUID id) {
        return toResponse(findEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> findByMosqueId(UUID mosqueId, Pageable pageable) {
        return reportRepository.findByMosqueId(mosqueId, pageable).map(this::toResponse);
    }

    @Transactional
    public ReportResponse create(ReportCreateRequest request) {
        Mosque mosque = mosqueRepository.findById(request.getMosqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Mosque", "id", request.getMosqueId()));
        User generatedBy = userRepository.findById(request.getGeneratedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getGeneratedBy()));

        Report report = Report.builder()
                .mosque(mosque)
                .generatedBy(generatedBy)
                .type(request.getType())
                .title(request.getTitle())
                .filters(request.getFilters())
                .fileUrl(request.getFileUrl())
                .generatedAt(Instant.now())
                .build();

        return toResponse(reportRepository.save(report));
    }

    @Transactional
    public ReportResponse update(UUID id, ReportUpdateRequest request) {
        Report report = findEntityOrThrow(id);

        if (request.getTitle() != null) {
            report.setTitle(request.getTitle());
        }
        if (request.getFilters() != null) {
            report.setFilters(request.getFilters());
        }
        if (request.getFileUrl() != null) {
            report.setFileUrl(request.getFileUrl());
        }

        return toResponse(reportRepository.save(report));
    }

    @Transactional
    public void delete(UUID id) {
        reportRepository.deleteById(id);
    }

    private Report findEntityOrThrow(UUID id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", id));
    }

    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .mosqueId(report.getMosque().getId())
                .generatedBy(report.getGeneratedBy().getId())
                .type(report.getType())
                .title(report.getTitle())
                .filters(report.getFilters())
                .fileUrl(report.getFileUrl())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
}
