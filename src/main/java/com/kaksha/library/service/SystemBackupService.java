package com.kaksha.library.service;

import com.kaksha.library.dto.SystemBackupRequest;
import com.kaksha.library.dto.SystemBackupResponse;
import com.kaksha.library.exception.BadRequestException;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.model.entity.SystemBackup;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.SystemBackupRepository;
import com.kaksha.library.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * System Backup Service for NFR-03 Availability Compliance.
 * Implements real database backup and restore using mysqldump/mysql commands.
 * Supports asynchronous processing and status tracking.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemBackupService {

    private final SystemBackupRepository backupRepository;
    private final UserRepository userRepository;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${app.backup.directory:backups}")
    private String backupDirectory;

    @Value("${app.backup.mysql.dump-path:mysqldump}")
    private String mysqldumpPath;

    @Value("${app.backup.mysql.restore-path:mysql}")
    private String mysqlPath;

    @Transactional(readOnly = true)
    public List<SystemBackupResponse> getAllBackups() {
        return backupRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemBackupResponse getBackupById(@NonNull Long id) {
        return backupRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", id));
    }

    @Transactional(readOnly = true)
    public SystemBackupResponse getLatestBackup() {
        return backupRepository.findFirstByStatusOrderByCreatedAtDesc(SystemBackup.BackupStatus.COMPLETED)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public BackupStatistics getBackupStatistics() {
        Long completedCount = backupRepository.countCompletedBackups();
        Long totalSize = backupRepository.getTotalBackupSize();
        
        return BackupStatistics.builder()
                .totalBackups(completedCount != null ? completedCount : 0L)
                .totalSize(totalSize != null ? totalSize : 0L)
                .build();
    }

    @Transactional
    public SystemBackupResponse createBackup(SystemBackupRequest request, String adminEmail) {
        log.info("Creating new backup '{}' by admin {}", request.getBackupName(), adminEmail);

        if (!backupEnabled) {
            throw new BadRequestException("Backup functionality is disabled");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found", 0L));

        SystemBackup.BackupType backupType;
        try {
            backupType = SystemBackup.BackupType.valueOf(request.getBackupType());
        } catch (Exception e) {
            backupType = SystemBackup.BackupType.FULL;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = request.getBackupName().replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + timestamp + ".sql";
        String filePath = backupDirectory + "/" + fileName;

        SystemBackup backup = SystemBackup.builder()
                .backupName(request.getBackupName())
                .filePath(filePath)
                .backupType(backupType)
                .status(SystemBackup.BackupStatus.PENDING)
                .description(request.getDescription())
                .tablesIncluded(request.getTablesIncluded())
                .createdBy(admin)
                .build();

        SystemBackup saved = backupRepository.save(Objects.requireNonNull(backup, "Backup cannot be null"));

        // Execute backup asynchronously
        executeBackupAsync(saved.getBackupId(), filePath, backupType);

        log.info("Backup created successfully with ID: {}", saved.getBackupId());
        return mapToResponse(saved);
    }

    @Async
    public CompletableFuture<Void> executeBackupAsync(@NonNull Long backupId, String filePath, SystemBackup.BackupType backupType) {
        log.info("Starting async backup execution for ID: {}", backupId);

        try {
            SystemBackup backup = backupRepository.findById(backupId).orElse(null);
            if (backup == null) {
                log.error("Backup not found for ID: {}", backupId);
                return CompletableFuture.completedFuture(null);
            }

            backup.setStatus(SystemBackup.BackupStatus.IN_PROGRESS);
            backupRepository.save(backup);

            // Create backup directory if it doesn't exist
            Path backupPath = Paths.get(filePath);
            Files.createDirectories(backupPath.getParent());

            // Extract database name from URL
            String dbName = extractDatabaseName(dbUrl);

            // Build mysqldump command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    mysqldumpPath,
                    "-u" + dbUsername,
                    "-p" + dbPassword,
                    "-h", extractHost(dbUrl),
                    "-P", extractPort(dbUrl),
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    dbName
            );

            processBuilder.redirectOutput(new File(filePath));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                File backupFile = new File(filePath);
                backup.setStatus(SystemBackup.BackupStatus.COMPLETED);
                backup.setCompletedAt(LocalDateTime.now());
                backup.setFileSize(backupFile.length());
                backupRepository.save(backup);
                log.info("Backup completed successfully for ID: {}", backupId);
            } else {
                String errorOutput;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    errorOutput = reader.lines().collect(Collectors.joining("\n"));
                }
                backup.setStatus(SystemBackup.BackupStatus.FAILED);
                backup.setErrorMessage("Backup process failed: " + errorOutput);
                backup.setCompletedAt(LocalDateTime.now());
                backupRepository.save(backup);
                log.error("Backup failed for ID: {} - {}", backupId, errorOutput);
            }

        } catch (Exception e) {
            log.error("Error during backup execution for ID: {}", backupId, e);
            updateBackupFailure(backupId, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    protected void updateBackupFailure(@NonNull Long backupId, String errorMessage) {
        try {
            SystemBackup backup = backupRepository.findById(backupId).orElse(null);
            if (backup != null) {
                backup.setStatus(SystemBackup.BackupStatus.FAILED);
                backup.setErrorMessage(errorMessage);
                backup.setCompletedAt(LocalDateTime.now());
                backupRepository.save(backup);
            }
        } catch (Exception e) {
            log.error("Failed to update backup failure status", e);
        }
    }

    @Transactional
    public SystemBackupResponse updateBackupStatus(@NonNull Long id, String status, String errorMessage) {
        log.info("Updating backup {} status to {}", id, status);

        SystemBackup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", id));

        SystemBackup.BackupStatus newStatus;
        try {
            newStatus = SystemBackup.BackupStatus.valueOf(status);
        } catch (Exception e) {
            throw new BadRequestException("Invalid backup status: " + status);
        }

        backup.setStatus(newStatus);
        if (errorMessage != null) {
            backup.setErrorMessage(errorMessage);
        }
        
        if (newStatus == SystemBackup.BackupStatus.COMPLETED || newStatus == SystemBackup.BackupStatus.FAILED) {
            backup.setCompletedAt(LocalDateTime.now());
        }

        SystemBackup updated = backupRepository.save(Objects.requireNonNull(backup, "Backup cannot be null"));
        log.info("Backup {} status updated to {}", id, status);
        
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteBackup(@NonNull Long id) {
        log.info("Deleting backup ID: {}", id);

        SystemBackup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", id));

        // Delete the actual backup file
        try {
            Path backupPath = Paths.get(backup.getFilePath());
            if (Files.exists(backupPath)) {
                Files.delete(backupPath);
                log.info("Deleted backup file: {}", backup.getFilePath());
            }
        } catch (Exception e) {
            log.warn("Could not delete backup file: {}", backup.getFilePath(), e);
        }

        backupRepository.delete(Objects.requireNonNull(backup, "Backup cannot be null"));
        log.info("Backup deleted successfully: {}", id);
    }

    @Transactional
    public SystemBackupResponse restoreBackup(@NonNull Long id, String adminEmail) {
        log.info("Restoring backup ID: {} by admin {}", id, adminEmail);

        SystemBackup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", id));

        if (backup.getStatus() != SystemBackup.BackupStatus.COMPLETED) {
            throw new BadRequestException("Cannot restore backup that is not completed");
        }

        // Verify backup file exists
        Path backupPath = Paths.get(backup.getFilePath());
        if (!Files.exists(backupPath)) {
            throw new BadRequestException("Backup file not found: " + backup.getFilePath());
        }

        // Execute restore asynchronously
        executeRestoreAsync(backup.getFilePath());

        backup.setStatus(SystemBackup.BackupStatus.RESTORED);
        backupRepository.save(backup);

        log.info("Restore initiated for backup ID: {}", id);
        return mapToResponse(backup);
    }

    @Async
    public CompletableFuture<Void> executeRestoreAsync(String filePath) {
        log.info("Starting async restore execution for file: {}", filePath);

        try {
            String dbName = extractDatabaseName(dbUrl);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    mysqlPath,
                    "-u" + dbUsername,
                    "-p" + dbPassword,
                    "-h", extractHost(dbUrl),
                    "-P", extractPort(dbUrl),
                    dbName
            );

            processBuilder.redirectInput(new File(filePath));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Restore completed successfully from file: {}", filePath);
            } else {
                String errorOutput;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    errorOutput = reader.lines().collect(Collectors.joining("\n"));
                }
                log.error("Restore failed: {}", errorOutput);
                throw new BadRequestException("Restore failed: " + errorOutput);
            }

        } catch (Exception e) {
            log.error("Error during restore execution", e);
            throw new BadRequestException("Restore failed: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    private String extractDatabaseName(String url) {
        // Parse jdbc:mysql://host:port/dbname?params
        String withoutParams = url.split("\\?")[0];
        int lastSlash = withoutParams.lastIndexOf('/');
        return withoutParams.substring(lastSlash + 1);
    }

    private String extractHost(String url) {
        // Parse jdbc:mysql://host:port/dbname
        String withoutProtocol = url.replace("jdbc:mysql://", "");
        String withoutParams = withoutProtocol.split("\\?")[0];
        String hostPort = withoutParams.split("/")[0];
        return hostPort.split(":")[0];
    }

    private String extractPort(String url) {
        // Parse jdbc:mysql://host:port/dbname
        String withoutProtocol = url.replace("jdbc:mysql://", "");
        String withoutParams = withoutProtocol.split("\\?")[0];
        String hostPort = withoutParams.split("/")[0];
        String[] parts = hostPort.split(":");
        return parts.length > 1 ? parts[1] : "3306";
    }

    private SystemBackupResponse mapToResponse(SystemBackup backup) {
        return SystemBackupResponse.builder()
                .backupId(backup.getBackupId())
                .backupName(backup.getBackupName())
                .filePath(backup.getFilePath())
                .fileSize(backup.getFileSize())
                .backupType(backup.getBackupType() != null ? backup.getBackupType().name() : null)
                .status(backup.getStatus() != null ? backup.getStatus().name() : null)
                .description(backup.getDescription())
                .tablesIncluded(backup.getTablesIncluded())
                .createdAt(backup.getCreatedAt())
                .completedAt(backup.getCompletedAt())
                .errorMessage(backup.getErrorMessage())
                .createdBy(backup.getCreatedBy() != null ? backup.getCreatedBy().getUsername() : null)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BackupStatistics {
        private Long totalBackups;
        private Long totalSize;
    }
}
