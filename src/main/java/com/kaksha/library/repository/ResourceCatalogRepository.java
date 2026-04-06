package com.kaksha.library.repository;

import com.kaksha.library.model.entity.ResourceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceCatalogRepository extends JpaRepository<ResourceCatalog, Long> {
    
    Optional<ResourceCatalog> findByCatalogName(String catalogName);
    
    boolean existsByCatalogName(String catalogName);
}
