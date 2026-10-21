package com.syncflow.worklog.infrastructure.persistence;

import com.syncflow.model.DailyWorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDailyWorkLogRepository extends JpaRepository<DailyWorkLog, Long> {
    Optional<DailyWorkLog> findByUserIdAndLogDate(UUID userId, LocalDate logDate);
    List<DailyWorkLog> findByLogDateOrderByCreatedAtDesc(LocalDate logDate);
    List<DailyWorkLog> findTop30ByOrderByLogDateDescCreatedAtDesc();
}
