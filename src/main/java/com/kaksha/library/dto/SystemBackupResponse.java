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
public class SystemBackupResponse {

    private Long backupId;
    private String backupName;
    private String filePath;
    private Long fileSize;
    private String backupType;
    private String status;
    private String description;
    private String tablesIncluded;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private String createdBy;
}
