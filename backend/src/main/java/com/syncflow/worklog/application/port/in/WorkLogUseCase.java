package com.syncflow.worklog.application.port.in;

import com.syncflow.dto.DailyWorkLogDTO;
import com.syncflow.dto.SubmitWorkLogRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WorkLogUseCase {
    DailyWorkLogDTO submitOrUpdateLog(UUID userId, SubmitWorkLogRequest request);
    DailyWorkLogDTO getMyLogForDate(UUID userId, LocalDate date);
    List<DailyWorkLogDTO> getLogsForDate(LocalDate date);
    List<DailyWorkLogDTO> getRecentLogs();
}
