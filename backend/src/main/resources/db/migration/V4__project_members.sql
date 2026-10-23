-- V4: projects get an explicit team. Only members (and admins) can see a project or be assigned its tasks.

CREATE TABLE project_members (
    id          bigint IDENTITY NOT NULL PRIMARY KEY,
    project_id  bigint NOT NULL,
    user_id     uniqueidentifier NOT NULL,
    role        varchar(20) NOT NULL,
    added_at    datetimeoffset(6) NOT NULL,
    CONSTRAINT FK_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT FK_project_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT UQ_project_members_project_user UNIQUE (project_id, user_id)
);
CREATE INDEX IX_project_members_user ON project_members (user_id);
GO

-- Existing projects had no team and were open to everyone. Keep that visibility: the manager
-- becomes MANAGER and every active user a MEMBER. Managers can then trim the team.
INSERT INTO project_members (project_id, user_id, role, added_at)
SELECT p.id, p.manager_id, 'MANAGER', SYSDATETIMEOFFSET()
FROM projects p
WHERE p.manager_id IS NOT NULL;

INSERT INTO project_members (project_id, user_id, role, added_at)
SELECT p.id, u.id, 'MEMBER', SYSDATETIMEOFFSET()
FROM projects p
CROSS JOIN users u
WHERE u.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM project_members m WHERE m.project_id = p.id AND m.user_id = u.id);
GO
