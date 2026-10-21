import { Injectable, signal, computed } from '@angular/core';
import { RbacService } from '../../../core/services/rbac.service';
import { Role, Permission } from '../../../core/ports/rbac.port';

@Injectable({
  providedIn: 'root'
})
export class RbacFacade {
  public roles = signal<Role[]>([]);
  public permissions = signal<Permission[]>([]);
  public selectedRoleId = signal<number | null>(null);
  public activePermissionIds = signal<Set<number>>(new Set());
  public isSaving = signal(false);
  public saveSuccessMessage = signal<string | null>(null);
  public bankUsers = signal<any[]>([]);

  // Search & Filter State
  public searchQuery = signal<string>('');
  public statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');

  // KPI Metrics
  public totalRolesCount = computed(() => this.roles().length);
  public activeRolesCount = computed(() => this.roles().filter(r => r.status !== false).length);
  public deactivatedRolesCount = computed(() => this.roles().filter(r => r.status === false).length);
  public totalAssignedUsersCount = computed(() => this.bankUsers().length);

  // Filtered Roles List
  public filteredRoles = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const filter = this.statusFilter();

    return this.roles().filter(role => {
      // Status filter
      if (filter === 'ACTIVE' && role.status === false) return false;
      if (filter === 'INACTIVE' && role.status !== false) return false;

      // Query filter
      if (!query) return true;
      const matchName = role.name.toLowerCase().includes(query);
      const matchDesc = (role.description || '').toLowerCase().includes(query);
      const matchPerms = (role.permissionAuthorities || []).some(a => a.toLowerCase().includes(query));
      return matchName || matchDesc || matchPerms;
    });
  });

  // Selected Role
  public selectedRole = computed(() => {
    const rId = this.selectedRoleId();
    return this.roles().find(r => r.id === rId) || null;
  });

  // Modules List
  public modules = computed(() => {
    const mods = new Set<string>();
    this.permissions().forEach(p => mods.add(p.module));
    return Array.from(mods);
  });

  // Create Custom Role Modal State
  public isCreateRoleModalOpen = signal(false);
  public newRoleName = signal('');
  public newRoleDescription = signal('');

  // Delete Role Modal State
  public isDeleteModalOpen = signal(false);
  public roleToDelete = signal<Role | null>(null);
  public deleteConfirmText = signal('');
  public deleteErrorMessage = signal<string | null>(null);

  constructor(private rbacService: RbacService) {}

  public init(): void {
    this.loadRolesAndPermissions();
    this.loadBankUsers();
  }

  public loadBankUsers(): void {
    this.rbacService.getAllUsers().subscribe({
      next: (users) => this.bankUsers.set(users),
      error: (err) => console.error('RbacFacade: Failed to load users', err)
    });
  }

  public changeUserRole(userId: string, roleId: number): void {
    this.rbacService.updateUserRole(userId, roleId).subscribe({
      next: (updatedUser) => {
        this.bankUsers.update(users =>
          users.map(u => u.id === updatedUser.id ? updatedUser : u)
        );
        this.loadRoles(); // refresh assigned users counts
        this.saveSuccessMessage.set(`Updated role for ${updatedUser.fullName || updatedUser.email}!`);
        setTimeout(() => this.saveSuccessMessage.set(null), 3000);
      },
      error: (err) => console.error('RbacFacade: Failed to update user role', err)
    });
  }

  public loadRolesAndPermissions(): void {
    this.rbacService.getAllPermissions().subscribe({
      next: (perms) => {
        this.permissions.set(perms);
        this.loadRoles();
      },
      error: (err) => console.error('RbacFacade: Failed to load permissions', err)
    });
  }

  public loadRoles(): void {
    this.rbacService.getAllRoles().subscribe({
      next: (rolesList) => {
        this.roles.set(rolesList);
        if (rolesList.length > 0 && this.selectedRoleId() === null) {
          this.selectRole(rolesList[0].id);
        } else if (this.selectedRoleId() !== null) {
          this.selectRole(this.selectedRoleId()!);
        }
      },
      error: (err) => console.error('RbacFacade: Failed to load roles', err)
    });
  }

  public selectRole(roleId: number): void {
    this.selectedRoleId.set(roleId);
    const role = this.roles().find(r => r.id === roleId);
    if (role) {
      this.activePermissionIds.set(new Set(role.permissionIds || []));
    }
  }

  public hasPermission(permissionId: number): boolean {
    return this.activePermissionIds().has(permissionId);
  }

  public togglePermission(permissionId: number): void {
    const current = new Set(this.activePermissionIds());
    if (current.has(permissionId)) {
      current.delete(permissionId);
    } else {
      current.add(permissionId);
    }
    this.activePermissionIds.set(current);
  }

  public getPermissionsForModule(moduleName: string): Permission[] {
    return this.permissions().filter(p => p.module === moduleName);
  }

  // Batch Selection Methods (like BanglaQR)
  public selectAllPermissions(): void {
    const all = new Set<number>(this.permissions().map(p => p.id));
    this.activePermissionIds.set(all);
  }

  public clearAllPermissions(): void {
    this.activePermissionIds.set(new Set<number>());
  }

  public isModuleAllSelected(moduleName: string): boolean {
    const perms = this.getPermissionsForModule(moduleName);
    if (perms.length === 0) return false;
    return perms.every(p => this.hasPermission(p.id));
  }

  public toggleModulePermissions(moduleName: string): void {
    const perms = this.getPermissionsForModule(moduleName);
    const isAll = this.isModuleAllSelected(moduleName);
    const current = new Set(this.activePermissionIds());

    if (isAll) {
      perms.forEach(p => current.delete(p.id));
    } else {
      perms.forEach(p => current.add(p.id));
    }
    this.activePermissionIds.set(current);
  }

  public saveRolePermissions(): void {
    const roleId = this.selectedRoleId();
    if (!roleId) return;

    this.isSaving.set(true);
    const permIds = Array.from(this.activePermissionIds());

    this.rbacService.updateRolePermissions(roleId, permIds).subscribe({
      next: (updatedRole) => {
        this.isSaving.set(false);
        this.roles.update(roles =>
          roles.map(r => r.id === updatedRole.id ? updatedRole : r)
        );
        this.saveSuccessMessage.set(`Permissions saved successfully for ${updatedRole.name}!`);
        setTimeout(() => this.saveSuccessMessage.set(null), 4000);
      },
      error: (err) => {
        this.isSaving.set(false);
        console.error('RbacFacade: Failed to update role permissions', err);
      }
    });
  }

  // Toggle Role Status (Active / Inactive)
  public toggleRoleStatus(roleId: number): void {
    this.rbacService.toggleRoleStatus(roleId).subscribe({
      next: (updatedRole) => {
        this.roles.update(roles =>
          roles.map(r => r.id === updatedRole.id ? updatedRole : r)
        );
        const statusStr = updatedRole.status ? 'Activated' : 'Deactivated';
        this.saveSuccessMessage.set(`Role '${updatedRole.name}' ${statusStr} successfully!`);
        setTimeout(() => this.saveSuccessMessage.set(null), 3000);
      },
      error: (err) => console.error('RbacFacade: Failed to toggle role status', err)
    });
  }

  // Create Custom Role Modal
  public openCreateRoleModal(): void {
    this.newRoleName.set('');
    this.newRoleDescription.set('');
    this.isCreateRoleModalOpen.set(true);
  }

  public closeCreateRoleModal(): void {
    this.isCreateRoleModalOpen.set(false);
  }

  public submitCreateRole(): void {
    const name = this.newRoleName().trim();
    if (!name) return;

    const desc = this.newRoleDescription().trim();
    const permIds = Array.from(this.activePermissionIds());

    this.rbacService.createCustomRole(name, desc, permIds).subscribe({
      next: (created) => {
        this.roles.update(r => [...r, created]);
        this.selectRole(created.id);
        this.closeCreateRoleModal();
      },
      error: (err) => console.error('RbacFacade: Failed to create role', err)
    });
  }

  // Delete Role Modal with Bank-Grade Safety
  public openDeleteModal(role: Role): void {
    this.roleToDelete.set(role);
    this.deleteConfirmText.set('');
    this.deleteErrorMessage.set(null);
    this.isDeleteModalOpen.set(true);
  }

  public closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.roleToDelete.set(null);
    this.deleteConfirmText.set('');
    this.deleteErrorMessage.set(null);
  }

  public confirmDeleteRole(): void {
    const role = this.roleToDelete();
    if (!role) return;

    if ((role.assignedUsersCount || 0) > 0) {
      this.deleteErrorMessage.set(`Cannot delete role: ${role.assignedUsersCount} member(s) are assigned to it.`);
      return;
    }

    if (this.deleteConfirmText().trim() !== 'DELETE') {
      this.deleteErrorMessage.set('Please type DELETE to confirm.');
      return;
    }

    this.rbacService.deleteRole(role.id).subscribe({
      next: () => {
        this.roles.update(roles => roles.filter(r => r.id !== role.id));
        if (this.selectedRoleId() === role.id) {
          const remaining = this.roles();
          if (remaining.length > 0) {
            this.selectRole(remaining[0].id);
          } else {
            this.selectedRoleId.set(null);
          }
        }
        this.closeDeleteModal();
        this.saveSuccessMessage.set(`Role '${role.name}' was permanently deleted!`);
        setTimeout(() => this.saveSuccessMessage.set(null), 3000);
      },
      error: (err) => {
        console.error('RbacFacade: Failed to delete role', err);
        this.deleteErrorMessage.set(err.error?.message || 'Failed to delete role.');
      }
    });
  }

  // Module Metadata Helpers
  public getModuleIcon(module: string): string {
    switch ((module || '').toUpperCase()) {
      case 'PROJECT': return 'folder-fill';
      case 'TASK': return 'check2-square';
      case 'STAGE': return 'diagram-3-fill';
      case 'CHAT': return 'chat-dots-fill';
      case 'MEETING': return 'camera-video-fill';
      case 'ADMIN': return 'shield-lock-fill';
      case 'WORK_LOG': return 'journal-text';
      case 'ANALYTICS': return 'graph-up-arrow';
      default: return 'grid-fill';
    }
  }

  public getModuleDescription(module: string): string {
    switch ((module || '').toUpperCase()) {
      case 'PROJECT': return 'Create and manage enterprise projects and workspace metadata';
      case 'TASK': return 'Author, edit, move, assign, and approve sprint tasks';
      case 'STAGE': return 'Orchestrate Kanban columns and workflow delivery pipelines';
      case 'CHAT': return 'Participate in team discussion channels and direct messaging';
      case 'MEETING': return 'Provision and host encrypted audio/video conference rooms';
      case 'ADMIN': return 'Govern security roles, permission matrices, and user accounts';
      case 'WORK_LOG': return 'Log daily standups, track completed tasks, and view team feeds';
      case 'ANALYTICS': return 'Inspect team velocity metrics, SLA compliance, and bottlenecks';
      default: return 'Core application feature permissions and operational controls';
    }
  }
}
