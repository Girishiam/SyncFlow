import { Observable } from 'rxjs';
import { DailyWorkLog, SubmitWorkLogPayload } from '../models/project.models';

export abstract class WorkLogPort {
  abstract submitWorkLog(payload: SubmitWorkLogPayload): Observable<DailyWorkLog>;
  abstract getMyWorkLog(date?: string): Observable<DailyWorkLog | null>;
  abstract getLogsByDate(date?: string): Observable<DailyWorkLog[]>;
  abstract getRecentLogs(): Observable<DailyWorkLog[]>;
}
