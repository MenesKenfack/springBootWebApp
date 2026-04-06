package com.kaksha.library.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "backups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Backup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backupID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;
    
    @Column(nullable = false)
    private LocalDateTime backupDate;
    
    @Column(nullable = false, length = 255)
    private String backupName;
    
    @Column(length = 255)
    private String backupPath;
    
    @Column(length = 50)
    private String backupType;
    
    @Column(length = 20)
    private String status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (backupDate == null) {
            backupDate = LocalDateTime.now();
        }
    }
}
