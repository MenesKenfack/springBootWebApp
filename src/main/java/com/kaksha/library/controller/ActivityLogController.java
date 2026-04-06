package com.kaksha.library.controller;

import com.kaksha.library.dto.ActivityLogResponse;
import com.kaksha.library.dto.ApiResponse;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.UserRepository;
import com.kaksha.library.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Slf4j
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Getting recent activities for user: {}", email);
        List<ActivityLogResponse> activities = activityLogService.getRecentActivities(user.getUserID(), limit);

        return ResponseEntity.ok(ApiResponse.success("Recent activities retrieved", activities));
    }

    @GetMapping("/since-last-login")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getActivitiesSinceLastLogin(
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Getting activities since last login for user: {}", email);
        List<ActivityLogResponse> activities = activityLogService.getActivitiesSinceLastLogin(user.getUserID());

        return ResponseEntity.ok(ApiResponse.success("Activities since last login retrieved", activities));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getSessionActivities(
            @PathVariable String sessionId,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Getting session activities for user: {}, session: {}", email, sessionId);
        List<ActivityLogResponse> activities = activityLogService.getActivitiesForSession(user.getUserID(), sessionId);

        return ResponseEntity.ok(ApiResponse.success("Session activities retrieved", activities));
    }
}
