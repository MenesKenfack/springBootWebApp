package com.kaksha.library.repository;

import com.kaksha.library.model.entity.ResourceAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceAccessLogRepository extends JpaRepository<ResourceAccessLog, Long> {
    
    long countByResourceResourceID(Long resourceId);
    
    @Query("SELECT a.resource.resourceID, COUNT(a) as accessCount " +
           "FROM ResourceAccessLog a " +
           "GROUP BY a.resource.resourceID " +
           "ORDER BY accessCount DESC")
    List<Object[]> findMostAccessedResources();
    
    @Query("SELECT a.resource.resourceID, COUNT(a) as accessCount " +
           "FROM ResourceAccessLog a " +
           "GROUP BY a.resource.resourceID " +
           "ORDER BY accessCount DESC")
    List<Object[]> findTopAccessedResources(@Param("limit") int limit);
}
