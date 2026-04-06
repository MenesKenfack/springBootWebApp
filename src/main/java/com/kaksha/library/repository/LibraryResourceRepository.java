package com.kaksha.library.repository;

import com.kaksha.library.model.entity.LibraryResource;
import com.kaksha.library.model.enums.ResourceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LibraryResourceRepository extends JpaRepository<LibraryResource, Long> {
    
    Page<LibraryResource> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    Page<LibraryResource> findByCategory(ResourceCategory category, Pageable pageable);
    
    Page<LibraryResource> findByAuthorContainingIgnoreCase(String author, Pageable pageable);
    
    @Query("SELECT r FROM LibraryResource r WHERE " +
           "LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<LibraryResource> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT r FROM LibraryResource r WHERE r.resourceCatalog.catalogId = :catalogId")
    Page<LibraryResource> findByCatalogId(@Param("catalogId") Long catalogId, Pageable pageable);
    
    // Year filter
    Page<LibraryResource> findByPublicationYear(Integer year, Pageable pageable);
    
    @Query("SELECT DISTINCT r.publicationYear FROM LibraryResource r WHERE r.publicationYear IS NOT NULL ORDER BY r.publicationYear DESC")
    List<Integer> findDistinctPublicationYears();
    
    // Price filters
    Page<LibraryResource> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<LibraryResource> findByPriceGreaterThanEqual(BigDecimal minPrice, Pageable pageable);
    Page<LibraryResource> findByPriceLessThanEqual(BigDecimal maxPrice, Pageable pageable);
    
    boolean existsByResourceCatalog_CatalogId(Long catalogId);
    
    long countByResourceCatalog_CatalogId(Long catalogId);
    
    List<LibraryResource> findTop5ByOrderByCreatedAtDesc();
    
    long countByIsPremiumOnlyTrue();
    
    long countByCategory(ResourceCategory category);
    
    @Query("SELECT COUNT(r) FROM LibraryResource r")
    long countTotalResources();
    
    @Query("SELECT r.category, COUNT(r) FROM LibraryResource r GROUP BY r.category")
    List<Object[]> countByCategory();
}
