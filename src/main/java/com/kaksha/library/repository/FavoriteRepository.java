package com.kaksha.library.repository;

import com.kaksha.library.model.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByClientUserID(Long clientId);

    Optional<Favorite> findByClientUserIDAndResourceResourceID(Long clientId, Long resourceId);

    boolean existsByClientUserIDAndResourceResourceID(Long clientId, Long resourceId);

    void deleteByClientUserIDAndResourceResourceID(Long clientId, Long resourceId);

    long countByClientUserID(Long clientId);
}
