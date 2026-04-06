package com.kaksha.library.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalyticsReportResponse {
    
    private String reportType;
    private String dateRange;
    private Long totalResources;
    private Long totalUsers;
    private Long newUsers;
    private BigDecimal totalRevenue;
    private Long totalPurchases;
    private Long totalResourceAccess;
    private List<DailyStats> dailyStats;
    private List<CategoryStats> categoryStats;
    private List<RecentPurchase> recentPurchases;
    
    @Data
    @Builder
    public static class DailyStats {
        private String date;
        private Long accesses;
        private Long purchases;
        private BigDecimal revenue;
    }
    
    @Data
    @Builder
    public static class CategoryStats {
        private String category;
        private Long count;
        private Long accesses;
    }
    
    @Data
    @Builder
    public static class RecentPurchase {
        private Long paymentId;
        private String resourceTitle;
        private String clientName;
        private BigDecimal amount;
        private String date;
    }
}
