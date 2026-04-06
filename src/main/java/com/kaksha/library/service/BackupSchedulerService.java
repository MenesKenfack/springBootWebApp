package com.kaksha.library.service;

import com.kaksha.library.dto.SystemBackupRequest;
import com.kaksha.library.model.entity.SystemBackup;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupSchedulerService {

    private final SystemBackupService backupService;
    private final UserRepository userRepository;
    private final BackupSettingsService backupSettingsService;

    /**
     * Scheduled monthly automatic backup.
     * Runs at 2:00 AM on the 1st day of every month.
     * Checks if auto-backup is enabled in settings before running.
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void performMonthlyBackup() {
        log.info("Checking scheduled monthly backup at {}", LocalDateTime.now());

        // Check if auto-backup is enabled
        if (!backupSettingsService.isAutoBackupEnabled()) {
            log.info("Automatic backup is disabled in settings. Skipping scheduled backup.");
            return;
        }

        log.info("Starting scheduled monthly backup");

        try {
            // Find a manager to set as the backup creator
            String managerEmail = userRepository.findAll().stream()
                    .filter(user -> user.getRole().name().equals("ROLE_MANAGER"))
                    .findFirst()
                    .map(User::getEmail)
                    .orElse("system@kaksha.library");

            // Create backup request
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            SystemBackupRequest request = SystemBackupRequest.builder()
                    .backupName("Monthly_Auto_Backup_" + timestamp)
                    .backupType(SystemBackup.BackupType.FULL.name())
                    .description("Automatic monthly full system backup")
                    .tablesIncluded("ALL")
                    .build();

            // Execute backup
            backupService.createBackup(request, managerEmail);

            log.info("Monthly automatic backup completed successfully");
        } catch (Exception e) {
            log.error("Failed to perform monthly automatic backup", e);
        }
    }

    /**
     * Manual trigger for testing or immediate backup needs.
     * Can be called via admin endpoint if needed.
     */
    public void triggerManualBackup(String adminEmail) {
        log.info("Manual backup triggered by {}", adminEmail);
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            SystemBackupRequest request = SystemBackupRequest.builder()
                    .backupName("Manual_Backup_" + timestamp)
                    .backupType(SystemBackup.BackupType.FULL.name())
                    .description("Manual backup triggered by admin")
                    .tablesIncluded("ALL")
                    .build();

            backupService.createBackup(request, adminEmail);
            log.info("Manual backup completed successfully");
        } catch (Exception e) {
            log.error("Failed to perform manual backup", e);
            throw e;
        }
    }
}
