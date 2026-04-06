package com.kaksha.library.repository;

import com.kaksha.library.model.entity.Backup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Long> {
    
    List<Backup> findByManagerUserIDOrderByCreatedAtDesc(Long managerId);
    
    List<Backup> findTop10ByOrderByCreatedAtDesc();
}
