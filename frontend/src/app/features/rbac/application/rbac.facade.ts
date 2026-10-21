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

  // Create Custom Role Modal State
  public isCreateRoleModalOpen = signal(false);
  public newRoleName = signal('');
  public newRoleDescription = signal('');

  public selectedRole = computed(() => {
    const rId = this.selectedRoleId();
    return this.roles().find(r => r.id === rId) || null;
  });

  public modules = computed(() => {
    const mods = new Set<string>();
    this.permissions().forEach(p => mods.add(p.module));
    return Array.from(mods);
  });

  constructor(private rbacService: RbacService) {}

  public init(): void {
    this.loadRolesAndPermissions();
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

  // Custom Role Creator
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
}
