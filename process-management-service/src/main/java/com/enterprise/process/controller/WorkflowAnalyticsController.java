package com.enterprise.process.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Workflow Analytics Controller
 * Provides endpoints for workflow performance metrics and analytics
 */
@RestController
@RequestMapping("/api/analytics/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowAnalyticsController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get approval time statistics by role
     */
    @GetMapping("/approval-time-by-role")
    public List<Map<String, Object>> getApprovalTimeByRole() {
        String sql = "SELECT * FROM v_approval_time_by_role ORDER BY total_approvals DESC";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get daily approval success rate
     */
    @GetMapping("/approval-success-rate")
    public List<Map<String, Object>> getApprovalSuccessRate(
            @RequestParam(defaultValue = "30") int days) {
        String sql = """
            SELECT 
                approval_date,
                total_products,
                approved_count,
                rejected_count,
                approval_rate_percent,
                rejection_rate_percent
            FROM v_approval_success_rate
            WHERE approval_date >= CURRENT_DATE - INTERVAL '? days'
            ORDER BY approval_date DESC
            """;
        return jdbcTemplate.queryForList(sql, days);
    }

    /**
     * Get workflow bottlenecks
     */
    @GetMapping("/bottlenecks")
    public List<Map<String, Object>> getWorkflowBottlenecks() {
        String sql = "SELECT * FROM v_workflow_bottlenecks";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get hourly approval throughput
     */
    @GetMapping("/throughput")
    public List<Map<String, Object>> getApprovalThroughput(
            @RequestParam(defaultValue = "7") int days) {
        String sql = """
            SELECT * FROM v_approval_throughput
            WHERE approval_hour >= CURRENT_TIMESTAMP - INTERVAL '? days'
            ORDER BY approval_hour DESC
            """;
        return jdbcTemplate.queryForList(sql, days);
    }

    /**
     * Get user performance metrics
     */
    @GetMapping("/user-performance")
    public List<Map<String, Object>> getUserPerformance(
            @RequestParam(required = false) String username) {
        if (username != null) {
            String sql = """
                SELECT * FROM v_user_approval_performance
                WHERE username = ?
                ORDER BY total_tasks_completed DESC
                """;
            return jdbcTemplate.queryForList(sql, username);
        } else {
            String sql = "SELECT * FROM v_user_approval_performance LIMIT 50";
            return jdbcTemplate.queryForList(sql);
        }
    }

    /**
     * Get process duration analysis
     */
    @GetMapping("/process-duration")
    public List<Map<String, Object>> getProcessDurationAnalysis(
            @RequestParam(defaultValue = "30") int days) {
        String sql = """
            SELECT * FROM v_process_duration_analysis
            WHERE process_date >= CURRENT_DATE - INTERVAL '? days'
            ORDER BY process_date DESC
            """;
        return jdbcTemplate.queryForList(sql, days);
    }

    /**
     * Get rejection analysis by category
     */
    @GetMapping("/rejections")
    public List<Map<String, Object>> getRejectionAnalysis(
            @RequestParam(defaultValue = "90") int days) {
        String sql = """
            SELECT * FROM v_rejection_analysis
            WHERE rejection_week >= CURRENT_DATE - INTERVAL '? days'
            ORDER BY rejection_week DESC, rejection_count DESC
            """;
        return jdbcTemplate.queryForList(sql, days);
    }

    /**
     * Get overall workflow statistics
     */
    @GetMapping("/statistics")
    public Map<String, Object> getOverallStatistics() {
        String sql = """
            SELECT 
                (SELECT COUNT(*) FROM act_ru_execution WHERE proc_def_key_ = 'product-approval-process') as active_processes,
                (SELECT COUNT(*) FROM act_ru_task) as pending_tasks,
                (SELECT COUNT(*) FROM act_hi_procinst WHERE proc_def_key_ = 'product-approval-process' AND end_time_ >= CURRENT_DATE) as completed_today,
                (SELECT AVG(EXTRACT(EPOCH FROM (end_time_ - start_time_))) FROM act_hi_procinst WHERE proc_def_key_ = 'product-approval-process' AND end_time_ >= CURRENT_DATE) as avg_completion_time_today,
                (SELECT COUNT(*) FROM products WHERE status = 'ACTIVE') as total_approved_products,
                (SELECT COUNT(*) FROM products WHERE status = 'REJECTED') as total_rejected_products
            """;
        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * Get approval funnel data
     */
    @GetMapping("/funnel")
    public Map<String, Object> getApprovalFunnel() {
        String sql = """
            SELECT 
                COUNT(*) FILTER (WHERE status = 'PENDING_APPROVAL') as pending_checker,
                COUNT(*) FILTER (WHERE status = 'PENDING_CONFIRMATION') as pending_confirmer,
                COUNT(*) FILTER (WHERE status = 'ACTIVE') as approved,
                COUNT(*) FILTER (WHERE status = 'REJECTED') as rejected
            FROM products
            WHERE process_instance_id IS NOT NULL
              AND created_at >= CURRENT_DATE - INTERVAL '30 days'
            """;
        return jdbcTemplate.queryForMap(sql);
    }
}
