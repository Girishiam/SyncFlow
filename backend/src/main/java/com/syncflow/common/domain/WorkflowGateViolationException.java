package com.syncflow.common.domain;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Thrown when a stage transition fails its quality gates and no valid override was supplied.
 * The violations are returned as {@code details} so the client can render them.
 */
public class WorkflowGateViolationException extends DomainException {
    public static final String CODE = "WORKFLOW_GATE_VIOLATION";

    public WorkflowGateViolationException(List<String> violations) {
        super(HttpStatus.CONFLICT, CODE, "Workflow gate requirements are not met", violations);
    }
}
