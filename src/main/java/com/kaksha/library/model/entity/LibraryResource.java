package com.kaksha.library.model.entity;

import com.kaksha.library.model.enums.ResourceCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "library_resources", indexes = {
    @Index(name = "idx_resource_id", columnList = "resourceID"),
    @Index(name = "idx_title", columnList = "title"),
    @Index(name = "idx_isbn", columnList = "isbn")
})
@SQLDelete(sql = "UPDATE library_resources SET deleted = true WHERE resourceID = ?")
@SQLRestriction("deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibraryResource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceID;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(length = 20)
    private String isbn;
    
    private Integer publicationYear;
    
    @Column(length = 255)
    private String author;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceCategory category;
    
    @Column(length = 255)
    private String resourceFile;
    
    @Column(length = 255)
    private String resourceImage;
    
    @Column(length = 2000)
    private String description;
    
    private BigDecimal price;
    
    private boolean isPremiumOnly = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", nullable = false)
    private ResourceCatalog resourceCatalog;
    
    @Column(nullable = false)
    private boolean deleted = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
