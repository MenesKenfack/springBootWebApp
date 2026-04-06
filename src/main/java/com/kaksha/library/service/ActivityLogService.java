package com.kaksha.library.service;

import com.kaksha.library.dto.ActivityLogResponse;
import com.kaksha.library.model.entity.ActivityLog;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.ActivityLogRepository;
import com.kaksha.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logActivity(Long userId, String activityType, String description, String resourceTitle, String sessionId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for activity logging: {}", userId);
            return;
        }

        ActivityLog activityLog = new ActivityLog();
        activityLog.setUserId(userId);
        activityLog.setUserEmail(user.getEmail());
        activityLog.setUserRole(user.getRole());
        activityLog.setActivityType(activityType);
        activityLog.setDescription(description);
        activityLog.setResourceTitle(resourceTitle);
        activityLog.setSessionId(sessionId);

        activityLogRepository.save(activityLog);
        log.info("Activity logged: {} - {} - {}", user.getEmail(), activityType, description);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getRecentActivities(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ActivityLog> activities = activityLogRepository.findRecentByUserId(userId, pageable);

        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getActivitiesSinceLastLogin(Long userId) {
        // Find the last LOGIN activity
        List<ActivityLog> loginActivities = activityLogRepository.findByUserIdAndActivityTypes(
                userId, List.of("LOGIN", "LOGOUT"));

        LocalDateTime lastLoginTime = loginActivities.stream()
                .filter(a -> "LOGIN".equals(a.getActivityType()))
                .map(ActivityLog::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusDays(7)); // Default to last 7 days if no login found

        // Get all activities since last login (excluding the login itself)
        List<ActivityLog> activities = activityLogRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, lastLoginTime);

        return activities.stream()
                .filter(a -> !"LOGIN".equals(a.getActivityType()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getActivitiesForSession(Long userId, String sessionId) {
        List<ActivityLog> activities = activityLogRepository.findByUserIdAndSessionId(userId, sessionId);

        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ActivityLogResponse mapToResponse(ActivityLog activity) {
        return ActivityLogResponse.builder()
                .logId(activity.getLogId())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .resourceTitle(activity.getResourceTitle())
                .timestamp(activity.getCreatedAt())
                .icon(getActivityIcon(activity.getActivityType()))
                .iconColor(getActivityIconColor(activity.getActivityType()))
                .formattedTime(formatTimeAgo(activity.getCreatedAt()))
                .build();
    }

    private String getActivityIcon(String activityType) {
        return switch (activityType) {
            case "LOGIN" -> "sign-in-alt";
            case "LOGOUT" -> "sign-out-alt";
            case "RESOURCE_VIEW" -> "eye";
            case "RESOURCE_PURCHASE" -> "shopping-cart";
            case "RESOURCE_READ" -> "book-open";
            case "FAVORITE_ADD" -> "heart";
            case "FAVORITE_REMOVE" -> "heart-broken";
            case "RATING_SUBMIT" -> "star";
            case "REVIEW_ADD" -> "comment";
            case "PROFILE_UPDATE" -> "user-edit";
            case "PASSWORD_CHANGE" -> "key";
            case "RESOURCE_CREATE" -> "file-upload";
            case "RESOURCE_UPDATE" -> "edit";
            case "RESOURCE_DELETE" -> "trash";
            case "USER_CREATE" -> "user-plus";
            case "USER_UPDATE" -> "user-cog";
            case "USER_DELETE" -> "user-times";
            case "BACKUP_CREATE" -> "database";
            case "BACKUP_RESTORE" -> "undo";
            case "REPORT_GENERATE" -> "file-alt";
            case "TERMS_UPDATE" -> "file-signature";
            default -> "circle";
        };
    }

    private String getActivityIconColor(String activityType) {
        return switch (activityType) {
            case "LOGIN" -> "#22C55E";  // Green
            case "LOGOUT" -> "#EF4444"; // Red
            case "RESOURCE_VIEW", "RESOURCE_READ" -> "#3B82F6"; // Blue
            case "RESOURCE_PURCHASE" -> "#4B5320"; // Olive
            case "FAVORITE_ADD" -> "#EF4444"; // Red
            case "FAVORITE_REMOVE" -> "#9CA3AF"; // Gray
            case "RATING_SUBMIT" -> "#F59E0B"; // Yellow
            case "REVIEW_ADD" -> "#8B5CF6"; // Purple
            case "PROFILE_UPDATE", "PASSWORD_CHANGE" -> "#22C55E"; // Green
            case "RESOURCE_CREATE" -> "#22C55E"; // Green
            case "RESOURCE_UPDATE" -> "#3B82F6"; // Blue
            case "RESOURCE_DELETE" -> "#EF4444"; // Red
            case "USER_CREATE" -> "#22C55E"; // Green
            case "USER_UPDATE" -> "#3B82F6"; // Blue
            case "USER_DELETE" -> "#EF4444"; // Red
            case "BACKUP_CREATE", "BACKUP_RESTORE" -> "#8B5CF6"; // Purple
            case "REPORT_GENERATE" -> "#3B82F6"; // Blue
            case "TERMS_UPDATE" -> "#F59E0B"; // Yellow
            default -> "#6B7280"; // Gray
        };
    }

    private String formatTimeAgo(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        }
    }
}
