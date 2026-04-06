package com.kaksha.library.dto;

import lombok.Data;

@Data
public class ReportRequest {
    
    private String dateRange; // LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, LAST_MONTH, CUSTOM
    private String startDate;
    private String endDate;
    private String reportType; // SUMMARY, DETAILED, REVENUE, USAGE
}
