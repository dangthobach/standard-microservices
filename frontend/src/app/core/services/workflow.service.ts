import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
    Task,
    ProcessInstance,
    ProcessDefinition,
    HistoricTask,
    TaskCompleteRequest,
    DelegateTaskRequest
} from '../models/workflow.model';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class WorkflowService {
    private flowApiUrl = `${environment.apiUrl}/process/flow`;
    private deploymentApiUrl = `${environment.apiUrl}/process/deployments`;

    constructor(private http: HttpClient) { }

    // ==================== Process Definitions ====================

    getProcessDefinitions(): Observable<ProcessDefinition[]> {
        return this.http.get<ProcessDefinition[]>(`${this.flowApiUrl}/process-definitions`);
    }

    getProcessBpmn(key: string): Observable<string> {
        return this.http.get(`${this.flowApiUrl}/process-definitions/${key}/bpmn`, { responseType: 'text' });
    }

    // ==================== Process Instances ====================

    startProcess(key: string, variables?: Record<string, any>): Observable<ProcessInstance> {
        return this.http.post<ProcessInstance>(`${this.flowApiUrl}/process/${key}/start`, variables || {});
    }

    getProcessInstances(): Observable<ProcessInstance[]> {
        return this.http.get<ProcessInstance[]>(`${this.flowApiUrl}/processes`);
    }

    getProcessInstancesByKey(processDefinitionKey: string): Observable<ProcessInstance[]> {
        const params = new HttpParams().set('processDefinitionKey', processDefinitionKey);
        return this.http.get<ProcessInstance[]>(`${this.flowApiUrl}/processes/by-key`, { params });
    }

    getProcessInstance(id: string): Observable<ProcessInstance> {
        return this.http.get<ProcessInstance>(`${this.flowApiUrl}/processes/${id}`);
    }

    getProcessVariables(processInstanceId: string): Observable<Record<string, any>> {
        return this.http.get<Record<string, any>>(`${this.flowApiUrl}/processes/${processInstanceId}/variables`);
    }

    updateProcessVariable(processInstanceId: string, variableName: string, value: any): Observable<void> {
        return this.http.put<void>(
            `${this.flowApiUrl}/processes/${processInstanceId}/variables/${variableName}`,
            { value }
        );
    }

    // ==================== Tasks ====================

    getTasks(): Observable<Task[]> {
        return this.http.get<Task[]>(`${this.flowApiUrl}/tasks`);
    }

    /**
     * Get tasks assigned to current user
     * Uses /tasks/my endpoint if available, otherwise falls back to /tasks
     */
    getMyTasks(): Observable<Task[]> {
        return this.http.get<Task[]>(`${this.flowApiUrl}/tasks/my`);
    }

    getTask(id: string): Observable<Task> {
        return this.http.get<Task>(`${this.flowApiUrl}/tasks/${id}`);
    }

    claimTask(taskId: string, userId: string): Observable<void> {
        return this.http.post<void>(`${this.flowApiUrl}/tasks/${taskId}/claim`, { userId });
    }

    completeTask(taskId: string, request: TaskCompleteRequest): Observable<void> {
        return this.http.post<void>(`${this.flowApiUrl}/tasks/${taskId}/complete`, request);
    }

    getTaskVariables(taskId: string): Observable<Record<string, any>> {
        return this.http.get<Record<string, any>>(`${this.flowApiUrl}/tasks/${taskId}/variables`);
    }

    setTaskVariables(taskId: string, variables: Record<string, any>): Observable<void> {
        return this.http.post<void>(`${this.flowApiUrl}/tasks/${taskId}/variables`, variables);
    }

    delegateTask(taskId: string, request: DelegateTaskRequest): Observable<void> {
        return this.http.post<void>(`${this.flowApiUrl}/tasks/${taskId}/delegate`, request);
    }

    unclaimTask(taskId: string): Observable<void> {
        return this.http.post<void>(`${this.flowApiUrl}/tasks/${taskId}/unclaim`, {});
    }

    resolveTask(taskId: string): Observable<void> {
        return this.http.post<void>(`${this.flowApiUrl}/tasks/${taskId}/resolve`, {});
    }

    // ==================== History ====================

    getProcessHistory(processInstanceId: string): Observable<HistoricTask[]> {
        return this.http.get<HistoricTask[]>(`${this.flowApiUrl}/history/process/${processInstanceId}`);
    }

    getTaskHistory(processInstanceId?: string): Observable<HistoricTask[]> {
        if (processInstanceId) {
            const params = new HttpParams().set('processInstanceId', processInstanceId);
            return this.http.get<HistoricTask[]>(`${this.flowApiUrl}/history/tasks`, { params });
        }
        return this.http.get<HistoricTask[]>(`${this.flowApiUrl}/history/tasks`);
    }

    // ==================== Deployment ====================

    deployProcess(file: File, processKey?: string, processName?: string): Observable<any> {
        const formData = new FormData();
        formData.append('file', file);
        if (processKey) formData.append('processKey', processKey);
        if (processName) formData.append('processName', processName);
        return this.http.post(`${this.flowApiUrl}/deploy`, formData);
    }

    deleteProcess(processDefinitionId: string): Observable<void> {
        return this.http.delete<void>(`${this.flowApiUrl}/process-definitions/${processDefinitionId}`);
    }
}
