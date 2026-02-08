export interface Task {
    id: string;
    name: string;
    description?: string;
    assignee?: string;
    owner?: string;
    createTime: string;
    dueDate?: string;
    priority: number;
    delegationState?: string;
    processInstanceId: string;
    processDefinitionId: string;
    taskDefinitionKey: string;
    formKey?: string;
    variables?: Record<string, any>;
}

export interface ProcessInstance {
    id: string;
    businessKey?: string;
    processDefinitionId: string;
    processDefinitionKey: string;
    processDefinitionName?: string;
    startTime: string;
    endTime?: string;
    startUserId?: string;
    suspended: boolean;
    variables?: Record<string, any>;
}

export interface ProcessDefinition {
    id: string;
    key: string;
    name: string;
    version: number;
    deploymentId: string;
    description?: string;
    suspended: boolean;
}

export interface HistoricTask {
    id: string;
    name: string;
    assignee?: string;
    startTime: string;
    endTime?: string;
    durationInMillis?: number;
    deleteReason?: string;
    processInstanceId: string;
    taskDefinitionKey: string;
}

export interface TaskCompleteRequest {
    approved?: boolean;
    comment?: string;
    variables?: Record<string, any>;
}

export interface DelegateTaskRequest {
    delegateUserId: string;
    comment?: string;
}
