package com.kaksha.library.model.entity;

import com.kaksha.library.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_session_id", columnList = "sessionId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole userRole;

    @Column(nullable = false, length = 50)
    private String activityType;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String resourceTitle;

    @Column(length = 255)
    private String sessionId;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public ActivityLog(Long userId, String userEmail, UserRole userRole, String activityType, String description) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.userRole = userRole;
        this.activityType = activityType;
        this.description = description;
    }
}
