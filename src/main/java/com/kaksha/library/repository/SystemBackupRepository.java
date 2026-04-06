package com.kaksha.library.repository;

import com.kaksha.library.model.entity.SystemBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemBackupRepository extends JpaRepository<SystemBackup, Long> {

    List<SystemBackup> findAllByOrderByCreatedAtDesc();

    List<SystemBackup> findByStatusOrderByCreatedAtDesc(SystemBackup.BackupStatus status);

    Optional<SystemBackup> findFirstByStatusOrderByCreatedAtDesc(SystemBackup.BackupStatus status);

    @Query("SELECT b FROM SystemBackup b WHERE b.status = 'COMPLETED' ORDER BY b.createdAt DESC")
    List<SystemBackup> findCompletedBackups();

    @Query("SELECT COUNT(b) FROM SystemBackup b WHERE b.status = 'COMPLETED'")
    Long countCompletedBackups();

    @Query("SELECT SUM(b.fileSize) FROM SystemBackup b WHERE b.status = 'COMPLETED'")
    Long getTotalBackupSize();
}
