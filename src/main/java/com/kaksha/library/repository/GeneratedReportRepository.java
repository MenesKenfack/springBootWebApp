package com.kaksha.library.repository;

import com.kaksha.library.model.entity.GeneratedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {
    List<GeneratedReport> findAllByOrderByCreatedAtDesc();
    List<GeneratedReport> findByCreatedBy_UserIDOrderByCreatedAtDesc(Long userId);
    List<GeneratedReport> findByStatusOrderByCreatedAtDesc(GeneratedReport.ReportStatus status);
}
