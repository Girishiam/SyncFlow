package com.syncflow.model.enums;

public enum AutomationTrigger {
    /** A task enters a review / QA stage. */
    MOVED_TO_REVIEW,
    /** A task is flagged as blocked. */
    TASK_BLOCKED,
    /** The last open checklist item of a task is completed. */
    ALL_SUBTASKS_COMPLETE,
    /** An active sprint is completed. */
    SPRINT_CLOSED
}
