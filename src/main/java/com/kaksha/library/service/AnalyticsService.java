package com.kaksha.library.service;

import com.kaksha.library.dto.AnalyticsReportResponse;
import com.kaksha.library.dto.DashboardStatsResponse;
import com.kaksha.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    
    private final LibraryResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PaymentsRepository paymentsRepository;
    
    public DashboardStatsResponse getDashboardStats(Long clientId) {
        log.info("Getting dashboard stats for client: {}", clientId);
        
        // Calculate this month's date range
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();
        
        // Global stats
        long totalResources = resourceRepository.countTotalResources();
        long totalUsers = userRepository.count();
        long newUsersThisMonth = clientRepository.countByCreatedAtAfter(startOfMonth);
        
        BigDecimal totalRevenue = paymentsRepository.sumTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        
        BigDecimal revenueThisMonth = paymentsRepository.sumRevenueBetweenDates(startOfMonth, now);
        if (revenueThisMonth == null) revenueThisMonth = BigDecimal.ZERO;
        
        long totalPurchases = paymentsRepository.countSuccessfulPayments();
        
        // Client-specific stats
        long totalResourcesAccessed = 0;
        long myPurchases = 0;
        Map<String, Long> resourcesByCategory = new HashMap<>();
        String favoriteCategory = null;
        
        if (clientId != null) {
            myPurchases = paymentsRepository.countByClientId(clientId);
            totalResourcesAccessed = myPurchases; // Using purchases as proxy for accessed resources
            
            // Get reading demographics
            List<Object[]> categoryData = paymentsRepository.getClientPurchasesByCategory(clientId);
            long maxCount = 0;
            for (Object[] row : categoryData) {
                String category = row[0].toString();
                Long count = ((Number) row[1]).longValue();
                resourcesByCategory.put(category, count);
                if (count > maxCount) {
                    maxCount = count;
                    favoriteCategory = category;
                }
            }
        }
        
        return DashboardStatsResponse.builder()
                .totalResources(totalResources)
                .totalUsers(totalUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .totalRevenue(totalRevenue)
                .revenueThisMonth(revenueThisMonth)
                .totalPurchases(totalPurchases)
                .totalResourcesAccessed(totalResourcesAccessed)
                .myPurchases(myPurchases)
                .resourcesByCategory(resourcesByCategory)
                .favoriteCategory(favoriteCategory)
                .build();
    }
    
    public AnalyticsReportResponse generateReport(String dateRange, String reportType) {
        log.info("Generating analytics report for range: {}, type: {}", dateRange, reportType);
        
        // Calculate date range
        LocalDateTime startDate = calculateStartDate(dateRange);
        LocalDateTime endDate = LocalDateTime.now();
        
        // Get metrics
        long totalResources = resourceRepository.countTotalResources();
        long totalUsers = userRepository.count();
        long newUsers = clientRepository.countByStatusTrue();
        BigDecimal totalRevenue = paymentsRepository.sumTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        
        long totalPurchases = paymentsRepository.countSuccessfulPayments();
        
        // Calculate revenue for period
        BigDecimal periodRevenue = paymentsRepository.sumRevenueBetweenDates(startDate, endDate);
        if (periodRevenue == null) periodRevenue = BigDecimal.ZERO;
        
        long periodPurchases = paymentsRepository.countPaymentsBetweenDates(startDate, endDate);
        
        // Build response
        return AnalyticsReportResponse.builder()
                .reportType(reportType)
                .dateRange(formatDateRange(startDate, endDate))
                .totalResources(totalResources)
                .totalUsers(totalUsers)
                .newUsers(newUsers)
                .totalRevenue(periodRevenue)
                .totalPurchases(periodPurchases)
                .totalResourceAccess(totalPurchases) // Using purchases as proxy for access
                .dailyStats(generateDailyStats(startDate, endDate))
                .categoryStats(generateCategoryStats())
                .recentPurchases(getRecentPurchases(5))
                .build();
    }
    
    private LocalDateTime calculateStartDate(String dateRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (dateRange != null ? dateRange.toUpperCase() : "LAST_30_DAYS") {
            case "LAST_7_DAYS" -> now.minusDays(7);
            case "THIS_MONTH" -> now.withDayOfMonth(1).withHour(0).withMinute(0);
            case "LAST_MONTH" -> now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
            case "LAST_90_DAYS" -> now.minusDays(90);
            default -> now.minusDays(30);
        };
    }
    
    private String formatDateRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return start.format(formatter) + " - " + end.format(formatter);
    }
    
    private List<AnalyticsReportResponse.DailyStats> generateDailyStats(LocalDateTime start, LocalDateTime end) {
        List<AnalyticsReportResponse.DailyStats> stats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Get actual revenue data from database
        List<Object[]> dailyRevenueData = paymentsRepository.getDailyRevenueStats(start, end);
        
        // Create a map of date -> [purchases, revenue]
        java.util.Map<String, Object[]> dataMap = new java.util.HashMap<>();
        for (Object[] row : dailyRevenueData) {
            String date = row[0].toString();
            Long purchases = ((Number) row[1]).longValue();
            BigDecimal revenue = row[2] != null ? BigDecimal.valueOf(((Number) row[2]).doubleValue()) : BigDecimal.ZERO;
            dataMap.put(date, new Object[]{purchases, revenue});
        }
        
        // Generate stats for each day in the range
        LocalDate current = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        
        while (!current.isAfter(endDate)) {
            String dateStr = current.format(formatter);
            Object[] dayData = dataMap.get(dateStr);
            
            Long purchases = 0L;
            BigDecimal revenue = BigDecimal.ZERO;
            Long accesses = 0L;
            
            if (dayData != null) {
                purchases = (Long) dayData[0];
                revenue = (BigDecimal) dayData[1];
                // Use purchases as proxy for accesses (each purchase implies access)
                accesses = purchases * 3; // Estimate: each purchase generates ~3 accesses
            }
            
            stats.add(AnalyticsReportResponse.DailyStats.builder()
                    .date(dateStr)
                    .accesses(accesses)
                    .purchases(purchases)
                    .revenue(revenue)
                    .build());
            current = current.plusDays(1);
        }
        
        return stats;
    }
    
    private List<AnalyticsReportResponse.CategoryStats> generateCategoryStats() {
        List<AnalyticsReportResponse.CategoryStats> stats = new ArrayList<>();
        
        var categoryCounts = resourceRepository.countByCategory();
        for (Object[] row : categoryCounts) {
            String category = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            
            stats.add(AnalyticsReportResponse.CategoryStats.builder()
                    .category(category)
                    .count(count)
                    .accesses(count * 5) // Estimated
                    .build());
        }
        
        return stats;
    }
    
    private List<AnalyticsReportResponse.RecentPurchase> getRecentPurchases(int limit) {
        var payments = paymentsRepository.findRecentSuccessfulPayments(PageRequest.of(0, limit));
        List<AnalyticsReportResponse.RecentPurchase> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        
        for (var payment : payments) {
            result.add(AnalyticsReportResponse.RecentPurchase.builder()
                    .paymentId(payment.getPaymentID())
                    .resourceTitle(payment.getResource() != null ? payment.getResource().getTitle() : "Unknown")
                    .clientName(payment.getClient() != null ? 
                            payment.getClient().getFirstName() + " " + payment.getClient().getLastName() : "Unknown")
                    .amount(payment.getAmount())
                    .date(payment.getPaidAt() != null ? payment.getPaidAt().format(formatter) : "N/A")
                    .build());
        }
        
        return result;
    }
}
