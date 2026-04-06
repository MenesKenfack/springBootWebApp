package com.kaksha.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemBackupRequest {

    @NotBlank(message = "Backup name is required")
    private String backupName;

    private String backupType;

    private String description;

    private String tablesIncluded;
}
