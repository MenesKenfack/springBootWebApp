package com.kaksha.library.controller;

import com.kaksha.library.dto.ApiResponse;
import com.kaksha.library.dto.GeneratedReportRequest;
import com.kaksha.library.dto.GeneratedReportResponse;
import com.kaksha.library.service.GeneratedReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class GeneratedReportController {

    private final GeneratedReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GeneratedReportResponse>>> getAllReports() {
        log.info("Get all generated reports request");
        List<GeneratedReportResponse> reports = reportService.getAllReports();
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully", reports));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GeneratedReportResponse>> getReportById(@PathVariable Long id) {
        log.info("Get report by ID: {}", id);
        GeneratedReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success("Report retrieved successfully", report));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GeneratedReportResponse>> createReport(
            @Valid @RequestBody GeneratedReportRequest request,
            Authentication authentication) {
        String managerEmail = authentication.getName();
        log.info("Create report request by manager: {}", managerEmail);
        
        GeneratedReportResponse report = reportService.createReport(request, managerEmail);
        return ResponseEntity.ok(ApiResponse.success("Report created successfully", report));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GeneratedReportResponse>> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody GeneratedReportRequest request) {
        log.info("Update report ID: {}", id);
        
        GeneratedReportResponse report = reportService.updateReport(id, request);
        return ResponseEntity.ok(ApiResponse.success("Report updated successfully", report));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        log.info("Delete report ID: {}", id);
        
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted successfully", null));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<ApiResponse<GeneratedReportResponse>> regenerateReport(
            @PathVariable Long id,
            Authentication authentication) {
        String managerEmail = authentication.getName();
        log.info("Regenerate report ID: {} by manager: {}", id, managerEmail);
        
        GeneratedReportResponse report = reportService.regenerateReport(id, managerEmail);
        return ResponseEntity.ok(ApiResponse.success("Report regenerated successfully", report));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> downloadReport(@PathVariable Long id) {
        log.info("Download report ID: {}", id);
        
        GeneratedReportResponse report = reportService.getReportById(id);
        // In a real implementation, this would return the file
        // For now, return the file path
        return ResponseEntity.ok(ApiResponse.success("Report download URL", report.getFilePath()));
    }
}
