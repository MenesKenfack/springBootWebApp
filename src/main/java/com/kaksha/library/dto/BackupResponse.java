package com.kaksha.library.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BackupResponse {
    
    private Long backupID;
    private LocalDateTime backupDate;
    private String backupName;
    private String backupPath;
    private String backupType;
    private String status;
    private LocalDateTime createdAt;
}
