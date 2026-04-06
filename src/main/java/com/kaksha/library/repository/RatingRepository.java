package com.kaksha.library.repository;

import com.kaksha.library.model.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByClientUserID(Long clientId);

    Optional<Rating> findByClientUserIDAndResourceResourceID(Long clientId, Long resourceId);

    boolean existsByClientUserIDAndResourceResourceID(Long clientId, Long resourceId);

    long countByClientUserID(Long clientId);

    List<Rating> findByResourceResourceID(Long resourceId);
}
