package com.kaksha.library.repository;

import com.kaksha.library.model.entity.Client;
import com.kaksha.library.model.entity.LibraryResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientResourceRepository extends JpaRepository<Client, Long> {
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Client c " +
           "JOIN c.accessedResources r WHERE c.userID = :clientId AND r.resourceID = :resourceId")
    boolean hasAccessToResource(@Param("clientId") Long clientId, @Param("resourceId") Long resourceId);
    
    @Modifying
    @Query(value = "INSERT INTO client_resources (client_id, resource_id) VALUES (:clientId, :resourceId)", nativeQuery = true)
    void grantAccess(@Param("clientId") Long clientId, @Param("resourceId") Long resourceId);
    
    @Query("SELECT r FROM Client c JOIN c.accessedResources r WHERE c.userID = :clientId")
    List<LibraryResource> findAccessibleResources(@Param("clientId") Long clientId);
}
