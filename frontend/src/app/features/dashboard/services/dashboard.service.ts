import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Activity, ActivityAction, PageResponse } from '../../../shared/models/activity.model';
import { Dashboard, DashboardFilters } from '../../../shared/models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private http = inject(HttpClient);

  getDashboard(filters: DashboardFilters): Observable<Dashboard> {
    return this.http.get<Dashboard>('/api/dashboard', { params: this.toParams(filters) });
  }

  /** Historial de actividad con los mismos filtros de empleado, tablero y fechas. */
  getActivity(filters: DashboardFilters, action: ActivityAction | null, page: number, size: number)
    : Observable<PageResponse<Activity>> {
    let params = this.toParams({ ...filters, boardType: null, boardStatus: null })
      .set('page', page)
      .set('size', size);
    if (action) {
      params = params.set('action', action);
    }
    return this.http.get<PageResponse<Activity>>('/api/activity', { params });
  }

  private toParams(filters: DashboardFilters): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
