package com.syncflow.security;

/**
 * Authority strings ({@code MODULE:ACTION}) checked by {@code @PreAuthorize} expressions.
 * Keep in sync with {@link com.syncflow.config.ReferenceDataInitializer}.
 */
public final class Permissions {

    private Permissions() {
    }

    public static final String PROJECT_VIEW = "PROJECT:VIEW";
    public static final String PROJECT_CREATE = "PROJECT:CREATE";
    public static final String PROJECT_EDIT = "PROJECT:EDIT";
    public static final String PROJECT_DELETE = "PROJECT:DELETE";

    public static final String TASK_VIEW = "TASK:VIEW";
    public static final String TASK_CREATE = "TASK:CREATE";
    public static final String TASK_EDIT = "TASK:EDIT";
    public static final String TASK_DELETE = "TASK:DELETE";
    public static final String TASK_APPROVE = "TASK:APPROVE";
    public static final String TASK_ASSIGN = "TASK:ASSIGN";
    public static final String TASK_OVERRIDE_GATE = "TASK:OVERRIDE_GATE";

    public static final String STAGE_BUILD = "STAGE:BUILD";
    public static final String SPRINT_MANAGE = "SPRINT:MANAGE";
    public static final String FINANCE_VIEW = "FINANCE:VIEW";
    public static final String AUDIT_VIEW = "AUDIT:VIEW";
    public static final String MEETING_HOST = "MEETING:HOST";

    public static final String CHAT_CREATE_CHANNEL = "CHAT:CREATE_CHANNEL";
    public static final String CHAT_EDIT_CHANNEL = "CHAT:EDIT_CHANNEL";
    public static final String CHAT_DELETE_CHANNEL = "CHAT:DELETE_CHANNEL";
    public static final String CHAT_MANAGE_MEMBERS = "CHAT:MANAGE_MEMBERS";

    public static final String ADMIN_MANAGE_USERS = "ADMIN:MANAGE_USERS";
    public static final String ADMIN_MANAGE_ROLES = "ADMIN:MANAGE_ROLES";

    public static final String ADMIN_ROLE = "ADMIN";
}
