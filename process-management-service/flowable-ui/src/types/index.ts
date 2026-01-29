export interface Task {
  id: string;
  name: string;
  description?: string;
  assignee?: string;
  created: string;
  dueDate?: string;
  processInstanceId?: string;
  taskDefinitionKey?: string;
  formKey?: string;
}

export interface ProcessInstance {
  id: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  businessKey?: string;
  startTime: string;
  endTime?: string;
  startUserId?: string;
  suspended: boolean;
  ended: boolean;
  state?: string;
  variables?: Record<string, any>;
}

export interface ProcessDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
  category?: string;
  suspended: boolean;
  deploymentId: string;
}

export interface Variable {
  name: string;
  value: any;
  type: string;
}

export interface BpmnProcess {
  id: string;
  name: string;
  xml: string;
}

// CMMN Types
export interface CaseDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
  category?: string;
  deploymentId: string;
  resourceName: string;
  suspended: boolean;
}

export interface CaseInstance {
  id: string;
  caseDefinitionId: string;
  businessKey?: string;
  startTime: string;
  startUserId?: string;
  variables?: Record<string, any>;
}

export interface PlanItemInstance {
  id: string;
  name: string;
  state: string;
  planItemDefinitionId: string;
  planItemDefinitionType: string;
  startTime?: string;
  endTime?: string;
}

export interface CaseTask {
  id: string;
  name: string;
  description?: string;
  assignee?: string;
  created: string;
  dueDate?: string;
  caseInstanceId?: string;
  planItemInstanceId?: string;
  variables?: Record<string, any>;
}

// DMN Types
export interface DecisionDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
  category?: string;
  deploymentId: string;
  resourceName: string;
}

export interface DecisionTable {
  id: string;
  key: string;
  name: string;
  version: number;
  category?: string;
  deploymentId: string;
  resourceName: string;
}

export interface DecisionExecution {
  id: string;
  decisionKey: string;
  decisionName: string;
  instanceId?: string;
  executionTime: string;
  endTime?: string;
  inputVariables?: Record<string, any>;
  outputVariables?: Record<string, any>;
}

export interface DecisionResult {
  decisionKey: string;
  result: Record<string, any>;
}

export interface DecisionResultAll {
  decisionKey: string;
  results: Array<{
    resultVariables: Record<string, any>;
    ruleId: string;
    ruleName: string;
  }>;
}

// Form Types
export interface FormDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
  deploymentId: string;
  resourceName: string;
}

// Pagination interface
export interface PaginatedResponse<T> {
  content?: T[];
  data?: T[];
  page: number;
  size: number;
  totalElements?: number;
  total?: number;
  totalPages: number;
}

// Batch operation result
export interface BatchResult {
  successful?: string[];
  failed?: string[];
  totalProcessed?: number;
  successCount?: number;
  failureCount?: number;
  success?: boolean;
  processed?: number;
  errors?: string[];
  results?: any[];
}

// Filter interfaces
export interface TaskFilter {
  assignee?: string;
  processInstanceId?: string;
  processDefinitionKey?: string;
  taskDefinitionKey?: string;
  createdAfter?: string;
  createdBefore?: string;
  dueAfter?: string;
  dueBefore?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface ProcessFilter {
  processDefinitionKey?: string;
  businessKey?: string;
  startedAfter?: string;
  startedBefore?: string;
  startedBy?: string;
  suspended?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

// Form interfaces
export interface FormField {
  id: string;
  name: string;
  type: 'string' | 'long' | 'boolean' | 'date' | 'enum' | 'custom';
  label?: string;
  defaultValue?: any;
  required?: boolean;
  readOnly?: boolean;
  properties?: Record<string, any>;
}

export interface TaskForm {
  taskId: string;
  formKey?: string;
  fields: FormField[];
}

// Real-time updates
export interface DashboardMetrics {
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  activeProcesses: number;
  completedProcesses: number;
  averageProcessingTime: number;
  tasksByStatus: Record<string, number>;
  processesByStatus: Record<string, number>;
  slaMetrics: {
    onTime: number;
    late: number;
    atRisk: number;
  };
}
