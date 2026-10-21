package com.syncflow.worklog.adapter.web;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.dto.DailyWorkLogDTO;
import com.syncflow.dto.SubmitWorkLogRequest;
import com.syncflow.model.User;
import com.syncflow.security.UserDetailsImpl;
import com.syncflow.worklog.application.port.in.WorkLogUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/worklogs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class WorkLogController {

    private final WorkLogUseCase workLogUseCase;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping
    public ResponseEntity<DailyWorkLogDTO> submitWorkLog(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SubmitWorkLogRequest request) {

        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.ok(workLogUseCase.submitOrUpdateLog(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<DailyWorkLogDTO> getMyWorkLog(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        UUID userId = resolveUserId(userDetails);
        DailyWorkLogDTO dto = workLogUseCase.getMyLogForDate(userId, date);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<DailyWorkLogDTO>> getLogsByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(workLogUseCase.getLogsForDate(date));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DailyWorkLogDTO>> getRecentLogs() {
        return ResponseEntity.ok(workLogUseCase.getRecentLogs());
    }

    private UUID resolveUserId(UserDetailsImpl userDetails) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        return userRepositoryPort.findAll().stream()
                .findFirst()
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("No active user available"));
    }
}
