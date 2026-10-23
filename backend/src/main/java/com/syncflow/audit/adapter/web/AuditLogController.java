package com.syncflow.audit.adapter.web;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.dto.AuditLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/audit-logs")
@org.springframework.security.access.prepost.PreAuthorize("@access.can('AUDIT:VIEW')")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogUseCase auditLogUseCase;

    @GetMapping
    public ResponseEntity<List<AuditLogDTO>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditLogUseCase.getAuditLogs(action, entityType, page, size));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAuditMetrics() {
        return ResponseEntity.ok(auditLogUseCase.getAuditMetrics());
    }

    /** Full re-verification of the tamper-evident chain (also runs nightly). */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyChain() {
        return ResponseEntity.ok(auditLogUseCase.verifyFullChain());
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType) {
        byte[] csvData = auditLogUseCase.exportAuditLogsCsv(action, entityType);
        String filename = "syncflow_audit_trail_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
