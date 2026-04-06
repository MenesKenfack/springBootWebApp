package com.kaksha.library.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_backups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backup_id")
    private Long backupId;

    @Column(name = "backup_name", nullable = false)
    private String backupName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "backup_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private BackupType backupType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BackupStatus status;

    @Column(name = "description")
    private String description;

    @Column(name = "tables_included")
    private String tablesIncluded;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = BackupStatus.PENDING;
        }
    }

    public enum BackupType {
        FULL,
        INCREMENTAL,
        SELECTIVE
    }

    public enum BackupStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        RESTORED
    }
}
