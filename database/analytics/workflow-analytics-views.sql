-- Workflow Analytics Views
-- Create materialized views for workflow performance analysis

-- ==================== APPROVAL PERFORMANCE VIEW ====================

-- Average approval time by role
CREATE OR REPLACE VIEW v_approval_time_by_role AS
SELECT 
    assignee_ as assignee_role,
    COUNT(*) as total_approvals,
    AVG(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as avg_approval_time_seconds,
    MIN(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as min_approval_time_seconds,
    MAX(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as max_approval_time_seconds,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (end_time_ - start_time_))) as median_approval_time_seconds,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (end_time_ - start_time_))) as p95_approval_time_seconds
FROM act_hi_taskinst
WHERE task_def_key_ IN ('checkerApprovalTask', 'confirmerApprovalTask', 'seniorApprovalTask', 'executiveApprovalTask')
  AND end_time_ IS NOT NULL
  AND start_time_ IS NOT NULL
GROUP BY assignee_;

COMMENT ON VIEW v_approval_time_by_role IS 'Average, min, max, median, and p95 approval times grouped by role';

-- ==================== APPROVAL SUCCESS RATE ====================

CREATE OR REPLACE VIEW v_approval_success_rate AS
SELECT 
    DATE_TRUNC('day', p.created_at) as approval_date,
    COUNT(*) as total_products,
    COUNT(CASE WHEN p.status = 'ACTIVE' THEN 1 END) as approved_count,
    COUNT(CASE WHEN p.status = 'REJECTED' THEN 1 END) as rejected_count,
    ROUND(COUNT(CASE WHEN p.status = 'ACTIVE' THEN 1 END) * 100.0 / COUNT(*), 2) as approval_rate_percent,
    ROUND(COUNT(CASE WHEN p.status = 'REJECTED' THEN 1 END) * 100.0 / COUNT(*), 2) as rejection_rate_percent
FROM products p
WHERE p.process_instance_id IS NOT NULL
  AND p.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', p.created_at)
ORDER BY approval_date DESC;

COMMENT ON VIEW v_approval_success_rate IS 'Daily approval and rejection rates for products in workflow';

-- ==================== WORKFLOW BOTTLENECK ANALYSIS ====================

CREATE OR REPLACE VIEW v_workflow_bottlenecks AS
SELECT 
    task_def_key_ as task_name,
    COUNT(*) as pending_count,
    AVG(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - create_time_))) as avg_wait_time_seconds,
    MAX(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - create_time_))) as max_wait_time_seconds,
    MIN(create_time_) as oldest_task_created_at
FROM act_ru_task
GROUP BY task_def_key_
ORDER BY pending_count DESC, avg_wait_time_seconds DESC;

COMMENT ON VIEW v_workflow_bottlenecks IS 'Identifies workflow bottlenecks by pending task count and wait time';

-- ==================== APPROVAL THROUGHPUT ====================

CREATE OR REPLACE VIEW v_approval_throughput AS
SELECT 
    DATE_TRUNC('hour', end_time_) as approval_hour,
    COUNT(*) as approvals_completed,
    AVG(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as avg_processing_time_seconds
FROM act_hi_taskinst
WHERE task_def_key_ IN ('checkerApprovalTask', 'confirmerApprovalTask')
  AND end_time_ IS NOT NULL
  AND end_time_ >= CURRENT_TIMESTAMP - INTERVAL '7 days'
GROUP BY DATE_TRUNC('hour', end_time_)
ORDER BY approval_hour DESC;

COMMENT ON VIEW v_approval_throughput IS 'Hourly approval throughput for the last 7 days';

-- ==================== USER PERFORMANCE ====================

CREATE OR REPLACE VIEW v_user_approval_performance AS
SELECT 
    assignee_ as username,
    task_def_key_ as task_type,
    COUNT(*) as total_tasks_completed,
    AVG(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as avg_completion_time_seconds,
    COUNT(CASE WHEN EXTRACT(EPOCH FROM (end_time_ - start_time_)) < 300 THEN 1 END) as completed_under_5min,
    COUNT(CASE WHEN EXTRACT(EPOCH FROM (end_time_ - start_time_)) > 3600 THEN 1 END) as completed_over_1hour
FROM act_hi_taskinst
WHERE end_time_ IS NOT NULL
  AND assignee_ IS NOT NULL
  AND end_time_ >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY assignee_, task_def_key_
ORDER BY total_tasks_completed DESC;

COMMENT ON VIEW v_user_approval_performance IS 'Individual user performance metrics for approval tasks';

-- ==================== PROCESS INSTANCE DURATION ====================

CREATE OR REPLACE VIEW v_process_duration_analysis AS
SELECT 
    DATE_TRUNC('day', start_time_) as process_date,
    COUNT(*) as total_processes,
    AVG(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as avg_duration_seconds,
    MIN(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as min_duration_seconds,
    MAX(EXTRACT(EPOCH FROM (end_time_ - start_time_))) as max_duration_seconds,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (end_time_ - start_time_))) as median_duration_seconds,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (end_time_ - start_time_))) as p95_duration_seconds
FROM act_hi_procinst
WHERE proc_def_key_ = 'product-approval-process'
  AND end_time_ IS NOT NULL
  AND start_time_ >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', start_time_)
ORDER BY process_date DESC;

COMMENT ON VIEW v_process_duration_analysis IS 'Process instance duration statistics by day';

-- ==================== REJECTION REASONS ====================

-- Note: This view assumes rejection reasons are stored in process variables
-- Adjust based on actual implementation

CREATE OR REPLACE VIEW v_rejection_analysis AS
SELECT 
    DATE_TRUNC('week', p.created_at) as rejection_week,
    p.category as product_category,
    COUNT(*) as rejection_count
FROM products p
WHERE p.status = 'REJECTED'
  AND p.created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY DATE_TRUNC('week', p.created_at), p.category
ORDER BY rejection_week DESC, rejection_count DESC;

COMMENT ON VIEW v_rejection_analysis IS 'Weekly rejection analysis by product category';

-- ==================== INDEXES FOR PERFORMANCE ====================

-- Ensure Flowable history tables have proper indexes
CREATE INDEX IF NOT EXISTS idx_hi_taskinst_end_time ON act_hi_taskinst(end_time_);
CREATE INDEX IF NOT EXISTS idx_hi_taskinst_task_def_key ON act_hi_taskinst(task_def_key_);
CREATE INDEX IF NOT EXISTS idx_hi_taskinst_assignee ON act_hi_taskinst(assignee_);
CREATE INDEX IF NOT EXISTS idx_hi_procinst_proc_def_key ON act_hi_procinst(proc_def_key_);
CREATE INDEX IF NOT EXISTS idx_ru_task_create_time ON act_ru_task(create_time_);
