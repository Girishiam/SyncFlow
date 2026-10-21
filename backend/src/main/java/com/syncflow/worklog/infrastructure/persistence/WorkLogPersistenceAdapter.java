package com.syncflow.worklog.infrastructure.persistence;

import com.syncflow.model.DailyWorkLog;
import com.syncflow.worklog.application.port.out.WorkLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkLogPersistenceAdapter implements WorkLogRepositoryPort {

    private final JpaDailyWorkLogRepository jpaDailyWorkLogRepository;

    @Override
    public Optional<DailyWorkLog> findByUserIdAndLogDate(UUID userId, LocalDate logDate) {
        return jpaDailyWorkLogRepository.findByUserIdAndLogDate(userId, logDate);
    }

    @Override
    public DailyWorkLog save(DailyWorkLog log) {
        return jpaDailyWorkLogRepository.save(log);
    }

    @Override
    public List<DailyWorkLog> findByLogDate(LocalDate logDate) {
        return jpaDailyWorkLogRepository.findByLogDateOrderByCreatedAtDesc(logDate);
    }

    @Override
    public List<DailyWorkLog> findRecentLogs() {
        return jpaDailyWorkLogRepository.findTop30ByOrderByLogDateDescCreatedAtDesc();
    }
}
