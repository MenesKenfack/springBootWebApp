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
public class GeneratedReportResponse {
    private Long reportId;
    private String reportName;
    private String reportType;
    private String dateRange;
    private String filePath;
    private Long fileSize;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime generatedAt;
    private String createdBy;
}
