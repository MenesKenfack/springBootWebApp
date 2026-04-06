package com.kaksha.library.service;

import com.kaksha.library.dto.BackupSettingsRequest;
import com.kaksha.library.dto.BackupSettingsResponse;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.model.entity.SystemBackupSettings;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.SystemBackupSettingsRepository;
import com.kaksha.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupSettingsService {

    private final SystemBackupSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public BackupSettingsResponse getSettings() {
        SystemBackupSettings settings = settingsRepository.findFirstByOrderBySettingIdAsc()
                .orElseGet(() -> createDefaultSettings());
        return mapToResponse(settings);
    }

    @Transactional
    public BackupSettingsResponse updateSettings(BackupSettingsRequest request, String adminEmail) {
        log.info("Updating backup settings by admin: {}", adminEmail);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found", 0L));

        SystemBackupSettings settings = settingsRepository.findFirstByOrderBySettingIdAsc()
                .orElseGet(() -> SystemBackupSettings.builder().build());

        settings.setAutoBackupEnabled(request.isAutoBackupEnabled());
        settings.setBackupFrequency(request.getBackupFrequency());
        settings.setBackupDayOfMonth(request.getBackupDayOfMonth());
        settings.setBackupTime(request.getBackupTime());
        settings.setBackupType(request.getBackupType());
        settings.setRetentionDays(request.getRetentionDays());
        settings.setModifiedBy(admin);

        SystemBackupSettings saved = settingsRepository.save(settings);
        log.info("Backup settings updated successfully");

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public boolean isAutoBackupEnabled() {
        return settingsRepository.findFirstByOrderBySettingIdAsc()
                .map(SystemBackupSettings::isAutoBackupEnabled)
                .orElse(true);
    }

    private SystemBackupSettings createDefaultSettings() {
        SystemBackupSettings defaultSettings = SystemBackupSettings.builder()
                .autoBackupEnabled(true)
                .backupFrequency("MONTHLY")
                .backupDayOfMonth(1)
                .backupTime("02:00")
                .backupType("FULL")
                .retentionDays(90)
                .build();
        return settingsRepository.save(Objects.requireNonNull(defaultSettings, "Settings cannot be null"));
    }

    private BackupSettingsResponse mapToResponse(SystemBackupSettings settings) {
        return BackupSettingsResponse.builder()
                .settingId(settings.getSettingId())
                .autoBackupEnabled(settings.isAutoBackupEnabled())
                .backupFrequency(settings.getBackupFrequency())
                .backupDayOfMonth(settings.getBackupDayOfMonth())
                .backupTime(settings.getBackupTime())
                .backupType(settings.getBackupType())
                .retentionDays(settings.getRetentionDays())
                .lastModifiedAt(settings.getLastModifiedAt())
                .modifiedBy(settings.getModifiedBy() != null ? settings.getModifiedBy().getUsername() : null)
                .build();
    }
}
