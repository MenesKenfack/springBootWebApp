package com.kaksha.library.controller;

import com.kaksha.library.dto.*;
import com.kaksha.library.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        log.info("Getting dashboard stats");
        
        // Get current user ID from authentication if available
        Long clientId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            // Try to get client ID from the user details if it's a client
            try {
                clientId = Long.valueOf(userDetails.getUsername());
            } catch (NumberFormatException e) {
                // Not a client ID, ignore
            }
        }
        
        DashboardStatsResponse stats = analyticsService.getDashboardStats(clientId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
    }
    
    @GetMapping("/generate")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<AnalyticsReportResponse>> generateReport(
            @RequestParam(required = false, defaultValue = "LAST_30_DAYS") String dateRange,
            @RequestParam(required = false, defaultValue = "SUMMARY") String reportType) {
        
        log.info("Generate analytics report - range: {}, type: {}", dateRange, reportType);
        AnalyticsReportResponse report = analyticsService.generateReport(dateRange, reportType);
        return ResponseEntity.ok(ApiResponse.success("Report generated successfully", report));
    }
}
