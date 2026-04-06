package com.kaksha.library.controller;

import com.kaksha.library.dto.ApiResponse;
import com.kaksha.library.dto.BackupSettingsRequest;
import com.kaksha.library.dto.BackupSettingsResponse;
import com.kaksha.library.service.BackupSchedulerService;
import com.kaksha.library.service.BackupSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Slf4j
public class BackupSettingsController {

    private final BackupSettingsService backupSettingsService;
    private final BackupSchedulerService backupSchedulerService;

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<BackupSettingsResponse>> getBackupSettings() {
        log.info("Get backup settings request");
        BackupSettingsResponse settings = backupSettingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("Backup settings retrieved", settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<BackupSettingsResponse>> updateBackupSettings(
            @RequestBody BackupSettingsRequest request,
            Authentication authentication) {
        String adminEmail = authentication.getName();
        log.info("Update backup settings request by admin: {}", adminEmail);
        
        BackupSettingsResponse settings = backupSettingsService.updateSettings(request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success("Backup settings updated", settings));
    }

    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<Void>> triggerManualBackup(Authentication authentication) {
        String adminEmail = authentication.getName();
        log.info("Manual backup triggered by admin: {}", adminEmail);
        
        backupSchedulerService.triggerManualBackup(adminEmail);
        return ResponseEntity.ok(ApiResponse.success("Manual backup triggered successfully", null));
    }
}
