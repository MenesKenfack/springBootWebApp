package com.kaksha.library.service;

import com.kaksha.library.dto.GeneratedReportRequest;
import com.kaksha.library.dto.GeneratedReportResponse;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.model.entity.GeneratedReport;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.GeneratedReportRepository;
import com.kaksha.library.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratedReportService {

    private final GeneratedReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GeneratedReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GeneratedReportResponse getReportById(@NonNull Long id) {
        return reportRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
    }

    @Transactional
    public GeneratedReportResponse createReport(GeneratedReportRequest request, String managerEmail) {
        log.info("Creating new report '{}' by manager {}", request.getReportName(), managerEmail);

        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Manager user not found", 0L));

        GeneratedReport.ReportType reportType;
        try {
            reportType = GeneratedReport.ReportType.valueOf(request.getReportType());
        } catch (Exception e) {
            reportType = GeneratedReport.ReportType.SUMMARY;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = request.getReportName().replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + timestamp + ".pdf";

        GeneratedReport report = GeneratedReport.builder()
                .reportName(request.getReportName())
                .reportType(reportType)
                .dateRange(request.getDateRange())
                .filePath("/reports/" + fileName)
                .description(request.getDescription())
                .status(GeneratedReport.ReportStatus.PENDING)
                .createdBy(manager)
                .build();

        GeneratedReport saved = reportRepository.save(Objects.requireNonNull(report, "Report cannot be null"));
        
        // Simulate report generation
        simulateReportGeneration(saved.getReportId());
        
        log.info("Report created successfully with ID: {}", saved.getReportId());
        return mapToResponse(saved);
    }

    @Transactional
    public GeneratedReportResponse updateReport(@NonNull Long id, GeneratedReportRequest request) {
        log.info("Updating report ID: {}", id);

        GeneratedReport report = reportRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));

        report.setReportName(request.getReportName());
        report.setDescription(request.getDescription());

        GeneratedReport updated = reportRepository.save(Objects.requireNonNull(report, "Report cannot be null"));
        log.info("Report updated successfully: {}", id);
        
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteReport(@NonNull Long id) {
        log.info("Deleting report ID: {}", id);

        GeneratedReport report = reportRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));

        reportRepository.delete(Objects.requireNonNull(report, "Report cannot be null"));
        log.info("Report deleted successfully: {}", id);
    }

    @Transactional
    public GeneratedReportResponse regenerateReport(@NonNull Long id, String managerEmail) {
        log.info("Regenerating report ID: {} by manager {}", id, managerEmail);

        GeneratedReport report = reportRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));

        report.setStatus(GeneratedReport.ReportStatus.PENDING);
        report.setGeneratedAt(null);
        
        GeneratedReport updated = reportRepository.save(Objects.requireNonNull(report, "Report cannot be null"));
        
        // Simulate report generation
        simulateReportGeneration(updated.getReportId());
        
        log.info("Report regenerated successfully: {}", id);
        return mapToResponse(updated);
    }

    private void simulateReportGeneration(@NonNull Long reportId) {
        try {
            GeneratedReport report = reportRepository.findById(Objects.requireNonNull(reportId, "Report ID cannot be null")).orElse(null);
            if (report != null) {
                report.setStatus(GeneratedReport.ReportStatus.GENERATING);
                reportRepository.save(report);
                
                // Simulate processing time
                report.setStatus(GeneratedReport.ReportStatus.COMPLETED);
                report.setGeneratedAt(LocalDateTime.now());
                report.setFileSize((long) (Math.random() * 10000000)); // Random size 0-10MB
                reportRepository.save(report);
            }
        } catch (Exception e) {
            log.error("Error during report generation", e);
        }
    }

    private GeneratedReportResponse mapToResponse(GeneratedReport report) {
        return GeneratedReportResponse.builder()
                .reportId(report.getReportId())
                .reportName(report.getReportName())
                .reportType(report.getReportType() != null ? report.getReportType().name() : null)
                .dateRange(report.getDateRange())
                .filePath(report.getFilePath())
                .fileSize(report.getFileSize())
                .status(report.getStatus() != null ? report.getStatus().name() : null)
                .description(report.getDescription())
                .createdAt(report.getCreatedAt())
                .generatedAt(report.getGeneratedAt())
                .createdBy(report.getCreatedBy() != null ? report.getCreatedBy().getUsername() : null)
                .build();
    }
}
