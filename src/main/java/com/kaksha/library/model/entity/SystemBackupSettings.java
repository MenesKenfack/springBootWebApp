package com.kaksha.library.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_backup_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemBackupSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "auto_backup_enabled", nullable = false)
    @Builder.Default
    private boolean autoBackupEnabled = true;

    @Column(name = "backup_frequency", nullable = false)
    @Builder.Default
    private String backupFrequency = "MONTHLY";

    @Column(name = "backup_day_of_month")
    @Builder.Default
    private Integer backupDayOfMonth = 1;

    @Column(name = "backup_time", nullable = false)
    @Builder.Default
    private String backupTime = "02:00";

    @Column(name = "backup_type", nullable = false)
    @Builder.Default
    private String backupType = "FULL";

    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 90;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    @PrePersist
    @PreUpdate
    protected void onModify() {
        lastModifiedAt = LocalDateTime.now();
    }
}
