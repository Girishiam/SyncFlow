package com.syncflow.worklog.application.port.out;

import com.syncflow.model.DailyWorkLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkLogRepositoryPort {
    Optional<DailyWorkLog> findByUserIdAndLogDate(UUID userId, LocalDate logDate);
    DailyWorkLog save(DailyWorkLog log);
    List<DailyWorkLog> findByLogDate(LocalDate logDate);
    List<DailyWorkLog> findRecentLogs();
}
