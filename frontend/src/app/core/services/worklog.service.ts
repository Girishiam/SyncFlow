import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DailyWorkLog, SubmitWorkLogPayload } from '../models/project.models';
import { WorkLogPort } from '../ports/worklog.port';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WorkLogService implements WorkLogPort {
  private readonly apiUrl = `${environment.apiBaseUrl}/v1/worklogs`;

  constructor(private http: HttpClient) {}

  submitWorkLog(payload: SubmitWorkLogPayload): Observable<DailyWorkLog> {
    return this.http.post<DailyWorkLog>(this.apiUrl, payload);
  }

  getMyWorkLog(date?: string): Observable<DailyWorkLog | null> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<DailyWorkLog | null>(`${this.apiUrl}/me`, { params });
  }

  getLogsByDate(date?: string): Observable<DailyWorkLog[]> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<DailyWorkLog[]>(this.apiUrl, { params });
  }

  getRecentLogs(): Observable<DailyWorkLog[]> {
    return this.http.get<DailyWorkLog[]>(`${this.apiUrl}/recent`);
  }
}
