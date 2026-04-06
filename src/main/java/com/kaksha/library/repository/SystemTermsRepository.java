package com.kaksha.library.repository;

import com.kaksha.library.model.entity.SystemTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemTermsRepository extends JpaRepository<SystemTerms, Long> {

    Optional<SystemTerms> findByIsActiveTrue();

    Optional<SystemTerms> findByVersion(String version);

    List<SystemTerms> findAllByOrderByCreatedAtDesc();

    boolean existsByVersion(String version);

    @Modifying
    @Query("UPDATE SystemTerms t SET t.isActive = false WHERE t.isActive = true")
    void deactivateAllTerms();

    @Query("SELECT t FROM SystemTerms t WHERE t.isActive = true")
    Optional<SystemTerms> findActiveTerms();
}
