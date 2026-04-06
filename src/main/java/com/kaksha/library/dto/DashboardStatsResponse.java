package com.kaksha.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Dashboard statistics response containing key metrics for the dashboard.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {
    
    // Client-specific stats
    private long totalResourcesAccessed;
    private long myPurchases;
    private long savedItemsCount;
    
    // Client reading demographics
    private Map<String, Long> resourcesByCategory;
    private String favoriteCategory;
    
    // Manager/Librarian stats
    private long newUsersThisMonth;
    private BigDecimal revenueThisMonth;
    
    // Global stats
    private long totalResources;
    private long totalUsers;
    private long totalPurchases;
    private BigDecimal totalRevenue;
}
