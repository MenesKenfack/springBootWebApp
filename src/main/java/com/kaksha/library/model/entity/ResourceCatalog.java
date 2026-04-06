package com.kaksha.library.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "resource_catalogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceCatalog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long catalogId;
    
    @Column(nullable = false, unique = true, length = 100)
    private String catalogName;
    
    @OneToMany(mappedBy = "resourceCatalog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LibraryResource> resources;
}
