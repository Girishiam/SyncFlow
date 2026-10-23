import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IRbacPort, Role, Permission } from '../ports/rbac.port';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class RbacService implements IRbacPort {
  private readonly apiUrl = `${environment.apiBaseUrl}/v1/rbac`;

  constructor(private http: HttpClient) {}

  getAllRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${this.apiUrl}/roles`);
  }

  getAllPermissions(): Observable<Permission[]> {
    return this.http.get<Permission[]>(`${this.apiUrl}/permissions`);
  }

  updateRolePermissions(roleId: number, permissionIds: number[]): Observable<Role> {
    return this.http.put<Role>(`${this.apiUrl}/roles/${roleId}/permissions`, { permissionIds });
  }

  createCustomRole(name: string, description: string, permissionIds: number[]): Observable<Role> {
    return this.http.post<Role>(`${this.apiUrl}/roles`, { name, description, permissionIds });
  }

  toggleRoleStatus(roleId: number): Observable<Role> {
    return this.http.put<Role>(`${this.apiUrl}/roles/${roleId}/toggle-status`, {});
  }

  deleteRole(roleId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/roles/${roleId}`);
  }

  getAllUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiBaseUrl}/v1/auth/users`);
  }

  updateUserRole(userId: string, roleId: number): Observable<any> {
    return this.http.put<any>(`${environment.apiBaseUrl}/v1/auth/users/${userId}/role`, { roleId });
  }
}
