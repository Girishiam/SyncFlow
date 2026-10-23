import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogItem, AuditMetrics } from '../models/project.models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly apiUrl = `${environment.apiBaseUrl}/v1/audit-logs`;

  constructor(private http: HttpClient) {}

  getAuditLogs(action?: string, entityType?: string, page: number = 0, size: number = 50): Observable<AuditLogItem[]> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (action && action !== 'ALL') {
      params = params.set('action', action);
    }
    if (entityType && entityType !== 'ALL') {
      params = params.set('entityType', entityType);
    }

    return this.http.get<AuditLogItem[]>(this.apiUrl, { params });
  }

  getAuditMetrics(): Observable<AuditMetrics> {
    return this.http.get<AuditMetrics>(`${this.apiUrl}/metrics`);
  }

  exportCsv(action?: string, entityType?: string): Observable<Blob> {
    let params = new HttpParams();
    if (action && action !== 'ALL') params = params.set('action', action);
    if (entityType && entityType !== 'ALL') params = params.set('entityType', entityType);

    return this.http.get(`${this.apiUrl}/export/csv`, {
      params,
      responseType: 'blob'
    });
  }
}
