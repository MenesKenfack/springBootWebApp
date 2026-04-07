package com.kaksha.library.repository;

import com.kaksha.library.model.entity.ResourceSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceSearchLogRepository extends JpaRepository<ResourceSearchLog, Long> {
    
    long countByResourceResourceID(Long resourceId);
    
    @Query("SELECT s.resource.resourceID, COUNT(s) as searchCount " +
           "FROM ResourceSearchLog s " +
           "GROUP BY s.resource.resourceID " +
           "ORDER BY searchCount DESC")
    List<Object[]> findMostSearchedResources();
    
    @Query("SELECT s.resource.resourceID, COUNT(s) as searchCount " +
           "FROM ResourceSearchLog s " +
           "GROUP BY s.resource.resourceID " +
           "ORDER BY searchCount DESC")
    List<Object[]> findTopSearchedResources(@Param("limit") int limit);
}
