import { Observable } from 'rxjs';

export interface Role {
  id: number;
  name: string;
  description?: string;
  isSystemRole: boolean;
  status?: boolean;
  assignedUsersCount?: number;
  permissionIds: number[];
  permissionAuthorities: string[];
}

export interface Permission {
  id: number;
  module: string;
  action: string;
  description?: string;
  authority: string;
}

export interface IRbacPort {
  getAllRoles(): Observable<Role[]>;
  getAllPermissions(): Observable<Permission[]>;
  updateRolePermissions(roleId: number, permissionIds: number[]): Observable<Role>;
  createCustomRole(name: string, description: string, permissionIds: number[]): Observable<Role>;
  toggleRoleStatus(roleId: number): Observable<Role>;
  deleteRole(roleId: number): Observable<void>;
  getAllUsers(): Observable<any[]>;
  updateUserRole(userId: string, roleId: number): Observable<any>;
}
