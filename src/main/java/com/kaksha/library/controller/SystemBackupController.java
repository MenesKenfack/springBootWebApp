package com.kaksha.library.controller;

import com.kaksha.library.dto.ApiResponse;
import com.kaksha.library.dto.SystemBackupRequest;
import com.kaksha.library.dto.SystemBackupResponse;
import com.kaksha.library.service.SystemBackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Slf4j
public class SystemBackupController {

    private final SystemBackupService backupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemBackupResponse>>> getAllBackups() {
        log.info("Fetching all backups");
        List<SystemBackupResponse> backups = backupService.getAllBackups();
        return ResponseEntity.ok(ApiResponse.success("Backups retrieved successfully", backups));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemBackupResponse>> getBackupById(@PathVariable Long id) {
        log.info("Fetching backup with ID: {}", id);
        SystemBackupResponse backup = backupService.getBackupById(id);
        return ResponseEntity.ok(ApiResponse.success("Backup retrieved successfully", backup));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<SystemBackupResponse>> getLatestBackup() {
        log.info("Fetching latest backup");
        SystemBackupResponse backup = backupService.getLatestBackup();
        if (backup != null) {
            return ResponseEntity.ok(ApiResponse.success("Latest backup retrieved successfully", backup));
        } else {
            return ResponseEntity.ok(ApiResponse.success("No backups found"));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackupStatistics() {
        log.info("Fetching backup statistics");
        SystemBackupService.BackupStatistics stats = backupService.getBackupStatistics();
        Map<String, Object> data = Map.of(
            "totalBackups", stats.getTotalBackups(),
            "totalSize", stats.getTotalSize()
        );
        return ResponseEntity.ok(ApiResponse.success("Backup statistics retrieved", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemBackupResponse>> createBackup(
            @Valid @RequestBody SystemBackupRequest request,
            Authentication authentication) {
        log.info("Creating new backup '{}' by {}", request.getBackupName(), authentication.getName());
        SystemBackupResponse created = backupService.createBackup(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Backup created successfully", created));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SystemBackupResponse>> updateBackupStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate,
            Authentication authentication) {
        log.info("Updating backup {} status by {}", id, authentication.getName());
        String status = statusUpdate.get("status");
        String errorMessage = statusUpdate.get("errorMessage");
        SystemBackupResponse updated = backupService.updateBackupStatus(id, status, errorMessage);
        return ResponseEntity.ok(ApiResponse.success("Backup status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("Deleting backup ID: {} by {}", id, authentication.getName());
        backupService.deleteBackup(id);
        return ResponseEntity.ok(ApiResponse.success("Backup deleted successfully"));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<SystemBackupResponse>> restoreBackup(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("Restoring backup ID: {} by {}", id, authentication.getName());
        SystemBackupResponse restored = backupService.restoreBackup(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Backup restored successfully", restored));
    }
}
