package com.kaksha.library.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_access_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceAccessLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accessLogId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private LibraryResource resource;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    
    @Column(length = 500)
    private String accessType; // VIEW, DOWNLOAD, PREVIEW
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
