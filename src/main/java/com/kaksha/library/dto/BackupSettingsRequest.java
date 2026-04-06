package com.kaksha.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupSettingsRequest {
    private boolean autoBackupEnabled;
    private String backupFrequency;
    private Integer backupDayOfMonth;
    private String backupTime;
    private String backupType;
    private Integer retentionDays;
}
