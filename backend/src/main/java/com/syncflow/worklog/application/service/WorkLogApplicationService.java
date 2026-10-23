package com.syncflow.worklog.application.service;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.dto.DailyWorkLogDTO;
import com.syncflow.dto.SubmitWorkLogRequest;
import com.syncflow.model.DailyWorkLog;
import com.syncflow.model.Task;
import com.syncflow.model.User;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import com.syncflow.worklog.application.port.in.WorkLogUseCase;
import com.syncflow.worklog.application.port.out.WorkLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkLogApplicationService implements WorkLogUseCase {

    private final WorkLogRepositoryPort workLogRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TaskRepositoryPort taskRepositoryPort;

    @Override
    @Transactional
    public DailyWorkLogDTO submitOrUpdateLog(UUID userId, SubmitWorkLogRequest request) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        LocalDate date = request.getLogDate() != null ? request.getLogDate() : LocalDate.now();

        int completedCount = (request.getCompletedTasksCount() != null && request.getCompletedTasksCount() > 0)
                ? request.getCompletedTasksCount()
                : calculateUserCompletedTasks(userId);

        DailyWorkLog logEntity = workLogRepositoryPort.findByUserIdAndLogDate(userId, date)
                .map(existing -> {
                    existing.setSummaryText(request.getSummaryText());
                    existing.setCompletedTasksCount(completedCount);
                    return existing;
                })
                .orElseGet(() -> DailyWorkLog.builder()
                        .user(user)
                        .logDate(date)
                        .summaryText(request.getSummaryText())
                        .completedTasksCount(completedCount)
                        .build());

        DailyWorkLog saved = workLogRepositoryPort.save(logEntity);
        log.info("Saved daily work log for user: {} on date: {}", user.getEmail(), date);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyWorkLogDTO getMyLogForDate(UUID userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return workLogRepositoryPort.findByUserIdAndLogDate(userId, targetDate)
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyWorkLogDTO> getLogsForDate(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return workLogRepositoryPort.findByLogDate(targetDate).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyWorkLogDTO> getRecentLogs() {
        return workLogRepositoryPort.findRecentLogs().stream()
                .map(this::mapToDTO)
                .toList();
    }

    private int calculateUserCompletedTasks(UUID userId) {
        List<Task> assignedTasks = taskRepositoryPort.findByAssigneeId(userId);
        if (assignedTasks == null) {
            return 0;
        }
        return (int) assignedTasks.stream()
                .filter(com.syncflow.task.application.service.TaskMapper::isDone)
                .count();
    }

    private DailyWorkLogDTO mapToDTO(DailyWorkLog entity) {
        return DailyWorkLogDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userFullName(entity.getUser().getFullName())
                .userEmail(entity.getUser().getEmail())
                .userRole(entity.getUser().getRole() != null ? entity.getUser().getRole().getName() : "USER")
                .logDate(entity.getLogDate())
                .summaryText(entity.getSummaryText())
                .completedTasksCount(entity.getCompletedTasksCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
