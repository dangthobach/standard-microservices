import axios from 'axios';
import { 
  Task, 
  ProcessInstance, 
  ProcessDefinition, 
  CaseDefinition,
  CaseInstance,
  PlanItemInstance,
  CaseTask,
  DecisionDefinition,
  DecisionTable,
  DecisionExecution,
  DecisionResultAll,
  PaginatedResponse,
  BatchResult
} from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const taskApi = {
  getTasks: async (): Promise<Task[]> => {
    const response = await api.get('/flow/tasks');
    return response.data as Task[];
  },
  getTask: async (taskId: string): Promise<Task> => {
    const response = await api.get(`/flow/tasks/${taskId}`);
    return response.data as Task;
  },
  
  // New paginated method with filtering
  getTasksPaginated: async (params: {
    page?: number;
    size?: number;
    assignee?: string;
    processInstanceId?: string;
    processDefinitionKey?: string;
    taskDefinitionKey?: string;
    createdAfter?: string;
    createdBefore?: string;
    dueAfter?: string;
    dueBefore?: string;
    sortBy?: string;
    sortOrder?: string;
  } = {}): Promise<PaginatedResponse<Task>> => {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.append(key, value.toString());
      }
    });
    
    const response = await api.get(`/batch/tasks?${searchParams.toString()}`);
    return response.data as PaginatedResponse<Task>;
  },

  claimTask: async (taskId: string, userId: string): Promise<void> => {
    await api.post(`/flow/tasks/${taskId}/claim`, { userId });
  },
  completeTask: async (taskId: string, variables?: Record<string, any>): Promise<void> => {
    await api.post(`/flow/tasks/${taskId}/complete`, variables || {});
  },
  getTaskVariables: async (taskId: string): Promise<Record<string, any>> => {
    const response = await api.get(`/flow/tasks/${taskId}/variables`);
    return response.data as Record<string, any>;
  },
  setTaskVariables: async (taskId: string, variables: Record<string, any>): Promise<void> => {
    await api.post(`/flow/tasks/${taskId}/variables`, variables);
  },

  // Batch operations
  claimTasksBatch: async (taskIds: string[], userId: string): Promise<BatchResult> => {
    const response = await api.post('/batch/tasks/claim', { taskIds, userId });
    return response.data as BatchResult;
  },

  completeTasksBatch: async (taskIds: string[], variables?: Record<string, any>): Promise<BatchResult> => {
    const response = await api.post('/batch/tasks/complete', { taskIds, variables: variables || {} });
    return response.data as BatchResult;
  },

  assignTasksBatch: async (assignments: Array<{ taskId: string; assignee: string }>): Promise<BatchResult> => {
    const response = await api.post('/batch/tasks/assign', { assignments });
    return response.data as BatchResult;
  },
};

export const processApi = {
  getProcesses: async (): Promise<ProcessDefinition[]> => {
    const response = await api.get('/flow/processes');
    return response.data as ProcessDefinition[];
  },
  startProcess: async (processKey: string, variables?: Record<string, any>): Promise<ProcessInstance> => {
    const response = await api.post(`/flow/processes/${processKey}/start`, variables || {});
    return response.data as ProcessInstance;
  },
  getProcessInstances: async (processKey?: string): Promise<ProcessInstance[]> => {
    const url = processKey 
      ? `/flow/processes/instances?processDefinitionKey=${processKey}`
      : '/flow/processes/instances';
    const response = await api.get(url);
    return response.data as ProcessInstance[];
  },
  getProcessInstance: async (instanceId: string): Promise<ProcessInstance> => {
    const response = await api.get(`/flow/instances/${instanceId}`);
    return response.data as ProcessInstance;
  },
  // Suspend process instance
  suspendProcessInstance: async (instanceId: string): Promise<void> => {
    await api.put(`/flow/instances/${instanceId}/suspend`);
  },
  // Activate process instance
  activateProcessInstance: async (instanceId: string): Promise<void> => {
    await api.put(`/flow/instances/${instanceId}/activate`);
  },
  // Delete process instance
  deleteProcessInstance: async (instanceId: string, deleteReason?: string): Promise<void> => {
    await api.delete(`/flow/instances/${instanceId}?deleteReason=${encodeURIComponent(deleteReason || 'Deleted by user')}`);
  },
  // Lấy BPMN XML của process
  getProcessBpmn: async (processKey: string): Promise<string> => {
    const response = await api.get(`/flow/processes/${processKey}/bpmn`);
    return response.data as string;
  },
  // Deploy process từ BPMN XML
  deployProcess: async (bpmnXml: string, processKey: string, processName: string): Promise<any> => {
    const formData = new FormData();
    const blob = new Blob([bpmnXml], { type: 'application/xml' });
    formData.append('file', blob, `${processKey}.bpmn`);
    formData.append('processKey', processKey);
    formData.append('processName', processName);
    
    const response = await api.post('/flow/deploy', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  // Xóa process definition
  deleteProcess: async (processId: string): Promise<void> => {
    await api.delete(`/flow/processes/${processId}`);
  },

  // Get task history for a process instance
  getTaskHistory: async (processInstanceId: string): Promise<any[]> => {
    const response = await api.get(`/flow/history/tasks?processInstanceId=${processInstanceId}`);
    return response.data as any[];
  },

  // Get process variables
  getProcessVariables: async (processInstanceId: string): Promise<any[]> => {
    const response = await api.get(`/flow/processes/instances/${processInstanceId}/variables`);
    const data = response.data;
    if (Array.isArray(data)) {
      return data;
    }
    if (data && typeof data === 'object') {
      return Object.entries(data).map(([name, value]) => ({ name, value }));
    }
    return [];
  },

  // Update process variable
  updateProcessVariable: async (processInstanceId: string, variableName: string, value: any): Promise<void> => {
    await api.put(`/flow/processes/instances/${processInstanceId}/variables/${variableName}`, { value });
  },
};

export const historyApi = {
  getProcessHistory: async (processInstanceId: string): Promise<any[]> => {
    const response = await api.get(`/flow/history/processes/${processInstanceId}`);
    return response.data as any[];
  },
  getTaskHistory: async (taskId: string): Promise<any[]> => {
    const response = await api.get(`/flow/history/tasks/${taskId}`);
    return response.data as any[];
  },
};

// CMMN API
export const cmmnApi = {
  // Case Definitions
  getCaseDefinitions: async (): Promise<CaseDefinition[]> => {
    const response = await api.get('/cmmn/definitions');
    return response.data as CaseDefinition[];
  },
  getCaseDefinition: async (key: string): Promise<CaseDefinition> => {
    const response = await api.get(`/cmmn/definitions/${key}`);
    return response.data as CaseDefinition;
  },
  
  // Case Instances
  startCase: async (caseKey: string, variables?: Record<string, any>): Promise<CaseInstance> => {
    const response = await api.post(`/cmmn/cases/${caseKey}/start`, variables || {});
    return response.data as CaseInstance;
  },
  getCaseInstances: async (): Promise<CaseInstance[]> => {
    const response = await api.get('/cmmn/cases');
    return response.data as CaseInstance[];
  },
  getCaseInstance: async (caseInstanceId: string): Promise<CaseInstance> => {
    const response = await api.get(`/cmmn/cases/${caseInstanceId}`);
    return response.data as CaseInstance;
  },
  terminateCase: async (caseInstanceId: string): Promise<void> => {
    await api.delete(`/cmmn/cases/${caseInstanceId}`);
  },
  
  // Plan Items
  getPlanItems: async (caseInstanceId: string): Promise<PlanItemInstance[]> => {
    const response = await api.get(`/cmmn/cases/${caseInstanceId}/plan-items`);
    return response.data as PlanItemInstance[];
  },
  startPlanItem: async (planItemId: string): Promise<void> => {
    await api.post(`/cmmn/plan-items/${planItemId}/start`);
  },
  completePlanItem: async (planItemId: string): Promise<void> => {
    await api.post(`/cmmn/plan-items/${planItemId}/complete`);
  },
  
  // Case Tasks
  getCaseTasks: async (): Promise<CaseTask[]> => {
    const response = await api.get('/cmmn/tasks');
    return response.data as CaseTask[];
  },
  getCaseTask: async (taskId: string): Promise<CaseTask> => {
    const response = await api.get(`/cmmn/tasks/${taskId}`);
    return response.data as CaseTask;
  },
  claimCaseTask: async (taskId: string, userId: string): Promise<void> => {
    await api.post(`/cmmn/tasks/${taskId}/claim`, { userId });
  },
  completeCaseTask: async (taskId: string, variables?: Record<string, any>): Promise<void> => {
    await api.post(`/cmmn/tasks/${taskId}/complete`, variables || {});
  },
  getCaseTaskVariables: async (taskId: string): Promise<Record<string, any>> => {
    const response = await api.get(`/cmmn/tasks/${taskId}/variables`);
    return response.data as Record<string, any>;
  },
  setCaseTaskVariables: async (taskId: string, variables: Record<string, any>): Promise<void> => {
    await api.post(`/cmmn/tasks/${taskId}/variables`, variables);
  },
  
  // History
  getHistoricCaseInstances: async (): Promise<CaseInstance[]> => {
    const response = await api.get('/cmmn/history/cases');
    return response.data as CaseInstance[];
  },
  getHistoricCaseInstance: async (caseInstanceId: string): Promise<CaseInstance> => {
    const response = await api.get(`/cmmn/history/cases/${caseInstanceId}`);
    return response.data as CaseInstance;
  },
  getHistoricPlanItems: async (caseInstanceId: string): Promise<PlanItemInstance[]> => {
    const response = await api.get(`/cmmn/history/plan-items/${caseInstanceId}`);
    return response.data as PlanItemInstance[];
  },
};

// DMN API
export const decisionApi = {
  // Decision Definitions
  getDecisions: async (): Promise<DecisionDefinition[]> => {
    const response = await api.get('/dmn/definitions');
    return response.data as DecisionDefinition[];
  },
  getDecision: async (key: string): Promise<DecisionDefinition> => {
    const response = await api.get(`/dmn/definitions/${key}`);
    return response.data as DecisionDefinition;
  },
  
  // Execute Decisions
  evaluateDecision: async (decisionKey: string, variables: Record<string, any>): Promise<any> => {
    const response = await api.post(`/dmn/decisions/${decisionKey}/execute`, variables);
    return response.data;
  },
  evaluateDecisionAll: async (decisionKey: string, variables: Record<string, any>): Promise<DecisionResultAll> => {
    const response = await api.post(`/dmn/decisions/${decisionKey}/execute-all`, variables);
    return response.data as DecisionResultAll;
  },
  
  // Deploy decision
  deployDecision: async (dmnXml: string, decisionKey: string, decisionName: string): Promise<any> => {
    const formData = new FormData();
    const blob = new Blob([dmnXml], { type: 'application/xml' });
    formData.append('file', blob, `${decisionKey}.dmn`);
    formData.append('decisionKey', decisionKey);
    formData.append('decisionName', decisionName);
    
    const response = await api.post('/dmn/deploy', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
  
  // Delete decision
  deleteDecision: async (decisionId: string): Promise<void> => {
    await api.delete(`/dmn/decisions/${decisionId}`);
  },
  
  // Decision Tables
  getDecisionTables: async (): Promise<DecisionTable[]> => {
    const response = await api.get('/dmn/tables');
    return response.data as DecisionTable[];
  },
  getDecisionTable: async (key: string): Promise<DecisionTable> => {
    const response = await api.get(`/dmn/tables/${key}`);
    return response.data as DecisionTable;
  },
  
  // History
  getDecisionHistory: async (): Promise<DecisionExecution[]> => {
    const response = await api.get('/dmn/history');
    return response.data as DecisionExecution[];
  },
  getDecisionExecution: async (executionId: string): Promise<DecisionExecution> => {
    const response = await api.get(`/dmn/history/${executionId}`);
    return response.data as DecisionExecution;
  },
};
