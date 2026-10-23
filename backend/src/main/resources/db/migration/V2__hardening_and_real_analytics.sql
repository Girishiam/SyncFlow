-- V2: security hardening, real analytics inputs, configurable workflow gates, stored attachments.

-- Refresh tokens (only SHA-256 hashes are stored)
CREATE TABLE refresh_tokens (
    id          bigint IDENTITY NOT NULL PRIMARY KEY,
    user_id     uniqueidentifier NOT NULL,
    token_hash  varchar(64) NOT NULL,
    expires_at  datetimeoffset(6) NOT NULL,
    revoked_at  datetimeoffset(6) NULL,
    created_at  datetimeoffset(6) NOT NULL,
    CONSTRAINT UK_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT FK_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX IX_refresh_tokens_user ON refresh_tokens (user_id);
GO

-- Task completion timestamp (drives burndown, velocity and cycle time)
ALTER TABLE tasks ADD completed_at datetimeoffset(6) NULL;
GO
UPDATE t SET t.completed_at = t.updated_at
FROM tasks t JOIN stages s ON s.id = t.stage_id
WHERE s.is_done_stage = 1 AND t.completed_at IS NULL;
GO

-- Configurable workflow gates per stage (previously inferred from stage names)
ALTER TABLE stages ADD requires_checklist_complete bit NOT NULL CONSTRAINT DF_stages_requires_checklist DEFAULT 0;
ALTER TABLE stages ADD requires_deliverable bit NOT NULL CONSTRAINT DF_stages_requires_deliverable DEFAULT 0;
ALTER TABLE stages ADD requires_no_blockers bit NOT NULL CONSTRAINT DF_stages_requires_no_blockers DEFAULT 0;
GO
-- Preserve the previous name-based behaviour for existing stages
UPDATE stages SET requires_checklist_complete = 1
WHERE LOWER(name) LIKE '%review%' OR LOWER(name) LIKE '%qa%';
UPDATE stages SET requires_deliverable = 1, requires_no_blockers = 1
WHERE is_done_stage = 1 OR LOWER(name) LIKE '%done%';
GO

-- Sprint closure snapshot for velocity
ALTER TABLE sprints ADD completed_at datetimeoffset(6) NULL;
ALTER TABLE sprints ADD completed_story_points int NULL;
GO

-- Stored attachment metadata (uploads are kept on disk, links keep using file_url)
ALTER TABLE task_attachments ADD storage_key varchar(255) NULL;
ALTER TABLE task_attachments ADD content_type varchar(150) NULL;
ALTER TABLE task_attachments ADD size_bytes bigint NULL;
GO

-- Tamper-evident audit hash chain
ALTER TABLE audit_logs ADD previous_hash varchar(64) NULL;
GO

-- Indexes for the hot query paths (SQL Server does not index foreign keys automatically)
CREATE INDEX IX_tasks_project ON tasks (project_id);
CREATE INDEX IX_tasks_stage ON tasks (stage_id);
CREATE INDEX IX_tasks_assignee ON tasks (assignee_id);
CREATE INDEX IX_tasks_sprint ON tasks (sprint_id);
CREATE INDEX IX_tasks_deadline ON tasks (deadline) WHERE deadline IS NOT NULL;
CREATE INDEX IX_messages_channel_id ON messages (channel_id, id);
CREATE INDEX IX_receipts_message ON message_seen_receipts (message_id);
CREATE INDEX IX_notifications_recipient ON notifications (recipient_id, is_read);
CREATE INDEX IX_task_time_logs_task ON task_time_logs (task_id);
CREATE INDEX IX_subtasks_task ON subtasks (task_id);
CREATE INDEX IX_task_attachments_task ON task_attachments (task_id);
CREATE INDEX IX_dependency_links_source ON task_dependency_links (source_task_id);
CREATE INDEX IX_dependency_links_target ON task_dependency_links (target_task_id);
CREATE INDEX IX_stages_project ON stages (project_id, position);
CREATE INDEX IX_sprints_project ON sprints (project_id, status);
GO
