/** Permission names as issued by the server (MODULE:ACTION). The server enforces them; the UI only hides what would fail. */
export const PERM = {
  PROJECT_CREATE: 'PROJECT:CREATE',
  PROJECT_EDIT: 'PROJECT:EDIT',
  TASK_CREATE: 'TASK:CREATE',
  TASK_EDIT: 'TASK:EDIT',
  TASK_ASSIGN: 'TASK:ASSIGN',
  STAGE_BUILD: 'STAGE:BUILD',
  SPRINT_MANAGE: 'SPRINT:MANAGE',
  FINANCE_VIEW: 'FINANCE:VIEW',
  AUDIT_VIEW: 'AUDIT:VIEW',
  MANAGE_ROLES: 'ADMIN:MANAGE_ROLES'
} as const;

export type PermissionName = typeof PERM[keyof typeof PERM];
