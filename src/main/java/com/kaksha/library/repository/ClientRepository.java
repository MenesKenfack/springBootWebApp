package com.kaksha.library.repository;

import com.kaksha.library.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    
    Optional<Client> findByEmail(String email);
    
    Optional<Client> findByUsername(String username);
    
    @Query("SELECT c FROM Client c LEFT JOIN FETCH c.accessedResources WHERE c.userID = :clientId")
    Optional<Client> findByIdWithResources(@Param("clientId") Long clientId);
    
    long countByStatusTrue();
    
    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") java.time.LocalDateTime date);
    
    // Dashboard stats - count premium tier users
    @Query("SELECT COUNT(c) FROM Client c WHERE c.userTier = 'PREMIUM'")
    long countByUserTierPremium();
}
