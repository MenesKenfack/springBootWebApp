package com.kaksha.library.repository;

import com.kaksha.library.model.entity.SystemBackupSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemBackupSettingsRepository extends JpaRepository<SystemBackupSettings, Long> {
    Optional<SystemBackupSettings> findFirstByOrderBySettingIdAsc();
}
