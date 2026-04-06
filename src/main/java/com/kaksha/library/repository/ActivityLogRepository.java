package com.kaksha.library.repository;

import com.kaksha.library.model.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ActivityLog> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime after);

    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId AND a.activityType IN :types ORDER BY a.createdAt DESC")
    List<ActivityLog> findByUserIdAndActivityTypes(@Param("userId") Long userId, @Param("types") List<String> types);

    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    List<ActivityLog> findRecentByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId AND a.sessionId = :sessionId ORDER BY a.createdAt DESC")
    List<ActivityLog> findByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}
