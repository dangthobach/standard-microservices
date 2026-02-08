export interface ApprovalTimeByRole {
    assignee_role: string;
    total_approvals: number;
    avg_approval_time_seconds: number;
    min_approval_time_seconds: number;
    max_approval_time_seconds: number;
    median_approval_time_seconds: number;
    p95_approval_time_seconds: number;
}

export interface ApprovalSuccessRate {
    approval_date: string;
    total_products: number;
    approved_count: number;
    rejected_count: number;
    approval_rate_percent: number;
    rejection_rate_percent: number;
}

export interface WorkflowBottleneck {
    task_name: string;
    pending_count: number;
    avg_wait_time_seconds: number;
    max_wait_time_seconds: number;
    oldest_task_created_at: string;
}

export interface UserPerformance {
    username: string;
    total_tasks_completed: number;
    avg_task_duration_seconds: number;
    approval_count: number;
    rejection_count: number;
}

export interface WorkflowStatistics {
    active_processes: number;
    pending_tasks: number;
    completed_today: number;
    avg_completion_time_today: number;
    total_approved_products: number;
    total_rejected_products: number;
}

export interface ApprovalFunnel {
    pending_checker: number;
    pending_confirmer: number;
    approved: number;
    rejected: number;
}

export interface DashboardStats {
    runningInstances: number;
    completedInstances24h: number;
    failedJobs: number;
    terminatedInstances: number;
    systemHealth: string;
    uptime: string;
}
