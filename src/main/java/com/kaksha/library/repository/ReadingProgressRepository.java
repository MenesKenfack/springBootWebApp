package com.kaksha.library.repository;

import com.kaksha.library.model.entity.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    List<ReadingProgress> findByClientUserID(Long clientId);

    Optional<ReadingProgress> findByClientUserIDAndResourceResourceID(Long clientId, Long resourceId);

    boolean existsByClientUserIDAndResourceResourceID(Long clientId, Long resourceId);

    long countByClientUserID(Long clientId);

    long countByClientUserIDAndProgressGreaterThan(Long clientId, Integer progress);
}
