import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
    ApprovalTimeByRole,
    ApprovalSuccessRate,
    WorkflowBottleneck,
    UserPerformance,
    WorkflowStatistics,
    ApprovalFunnel,
    DashboardStats
} from '../models/workflow-analytics.model';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class WorkflowAnalyticsService {
    private analyticsApiUrl = `${environment.apiUrl}/process/analytics`;
    private dashboardApiUrl = `${environment.apiUrl}/process/dashboard`;

    constructor(private http: HttpClient) { }

    // ==================== Dashboard Stats ====================

    getDashboardStats(): Observable<DashboardStats> {
        return this.http.get<DashboardStats>(`${this.dashboardApiUrl}/stats`);
    }

    // ==================== Workflow Analytics ====================

    getApprovalTimeByRole(): Observable<ApprovalTimeByRole[]> {
        return this.http.get<ApprovalTimeByRole[]>(`${this.analyticsApiUrl}/workflow/approval-time-by-role`);
    }

    getApprovalSuccessRate(days: number = 30): Observable<ApprovalSuccessRate[]> {
        const params = new HttpParams().set('days', days.toString());
        return this.http.get<ApprovalSuccessRate[]>(`${this.analyticsApiUrl}/workflow/approval-success-rate`, { params });
    }

    getWorkflowBottlenecks(): Observable<WorkflowBottleneck[]> {
        return this.http.get<WorkflowBottleneck[]>(`${this.analyticsApiUrl}/workflow/bottlenecks`);
    }

    getApprovalThroughput(days: number = 7): Observable<any[]> {
        const params = new HttpParams().set('days', days.toString());
        return this.http.get<any[]>(`${this.analyticsApiUrl}/workflow/throughput`, { params });
    }

    getUserPerformance(username?: string): Observable<UserPerformance[]> {
        if (username) {
            const params = new HttpParams().set('username', username);
            return this.http.get<UserPerformance[]>(`${this.analyticsApiUrl}/workflow/user-performance`, { params });
        }
        return this.http.get<UserPerformance[]>(`${this.analyticsApiUrl}/workflow/user-performance`);
    }

    getProcessDurationAnalysis(days: number = 30): Observable<any[]> {
        const params = new HttpParams().set('days', days.toString());
        return this.http.get<any[]>(`${this.analyticsApiUrl}/workflow/process-duration`, { params });
    }

    getRejectionAnalysis(days: number = 90): Observable<any[]> {
        const params = new HttpParams().set('days', days.toString());
        return this.http.get<any[]>(`${this.analyticsApiUrl}/workflow/rejections`, { params });
    }

    getOverallStatistics(): Observable<WorkflowStatistics> {
        return this.http.get<WorkflowStatistics>(`${this.analyticsApiUrl}/workflow/statistics`);
    }

    getApprovalFunnel(): Observable<ApprovalFunnel> {
        return this.http.get<ApprovalFunnel>(`${this.analyticsApiUrl}/workflow/funnel`);
    }

    // ==================== General Analytics ====================

    getDashboardMetrics(): Observable<any> {
        return this.http.get<any>(`${this.analyticsApiUrl}/dashboard`);
    }

    getPerformanceMetrics(): Observable<any> {
        return this.http.get<any>(`${this.analyticsApiUrl}/performance`);
    }

    getSLAMetrics(): Observable<any> {
        return this.http.get<any>(`${this.analyticsApiUrl}/sla`);
    }

    getTaskTrends(days: number = 7): Observable<any[]> {
        const params = new HttpParams().set('days', days.toString());
        return this.http.get<any[]>(`${this.analyticsApiUrl}/task-trends`, { params });
    }

    getProcessTrends(days: number = 7): Observable<any[]> {
        const params = new HttpParams().set('days', days.toString());
        return this.http.get<any[]>(`${this.analyticsApiUrl}/process-trends`, { params });
    }

    getRealtimeMetrics(): Observable<any> {
        return this.http.get<any>(`${this.analyticsApiUrl}/realtime`);
    }
}
