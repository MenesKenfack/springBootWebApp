package com.kaksha.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupSettingsResponse {
    private Long settingId;
    private boolean autoBackupEnabled;
    private String backupFrequency;
    private Integer backupDayOfMonth;
    private String backupTime;
    private String backupType;
    private Integer retentionDays;
    private LocalDateTime lastModifiedAt;
    private String modifiedBy;
}
