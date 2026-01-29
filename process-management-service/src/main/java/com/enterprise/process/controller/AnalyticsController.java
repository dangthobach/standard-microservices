package com.enterprise.process.controller;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.dmn.api.DmnHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import com.enterprise.process.websocket.NotificationService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:3000")
@EnableAsync
public class AnalyticsController {

    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private HistoryService historyService;
    
    @Autowired
    private RepositoryService repositoryService;
    
    @Autowired
    private CmmnRuntimeService cmmnRuntimeService;
    
    @Autowired
    private CmmnTaskService cmmnTaskService;
    
    @Autowired
    private DmnHistoryService dmnHistoryService;
    
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Task metrics
            long totalTasks = taskService.createTaskQuery().count();
            long unassignedTasks = taskService.createTaskQuery().taskUnassigned().count();
            long assignedTasks = totalTasks - unassignedTasks;
            
            // Process metrics
            long activeProcesses = runtimeService.createProcessInstanceQuery().active().count();
            long suspendedProcesses = runtimeService.createProcessInstanceQuery().suspended().count();
            long completedProcesses = historyService.createHistoricProcessInstanceQuery().finished().count();
            
            // Case metrics (CMMN)
            long activeCases = cmmnRuntimeService.createCaseInstanceQuery().count();
            long caseTasks = cmmnTaskService.createTaskQuery().count();
            
            // Decision metrics (DMN)
            long decisionExecutions = dmnHistoryService.createHistoricDecisionExecutionQuery().count();
            
            // Calculate SLA metrics (mock data for now - you can implement real SLA logic)
            Map<String, Object> slaMetrics = calculateSLAMetrics();
            
            // Performance metrics
            Map<String, Object> performanceMetrics = calculatePerformanceMetrics();
            
            // Recent activities
            List<Map<String, Object>> recentActivities = getRecentActivities();
            
            metrics.put("totalTasks", totalTasks);
            metrics.put("completedTasks", assignedTasks); // Simplified - assigned tasks as proxy for completed
            metrics.put("pendingTasks", unassignedTasks);
            metrics.put("activeProcesses", activeProcesses);
            metrics.put("completedProcesses", completedProcesses);
            metrics.put("suspendedProcesses", suspendedProcesses);
            metrics.put("activeCases", activeCases);
            metrics.put("caseTasks", caseTasks);
            metrics.put("decisionExecutions", decisionExecutions);
            metrics.put("averageProcessingTime", calculateAverageProcessingTime());
            
            // Task distribution
            Map<String, Long> tasksByStatus = new HashMap<>();
            tasksByStatus.put("Assigned", assignedTasks);
            tasksByStatus.put("Unassigned", unassignedTasks);
            metrics.put("tasksByStatus", tasksByStatus);
            
            // Process distribution
            Map<String, Long> processesByStatus = new HashMap<>();
            processesByStatus.put("Active", activeProcesses);
            processesByStatus.put("Suspended", suspendedProcesses);
            processesByStatus.put("Completed", completedProcesses);
            metrics.put("processesByStatus", processesByStatus);
            
            metrics.put("slaMetrics", slaMetrics);
            metrics.put("performanceMetrics", performanceMetrics);
            metrics.put("recentActivities", recentActivities);
            metrics.put("timestamp", LocalDateTime.now().toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            metrics.put("error", "Failed to calculate metrics: " + e.getMessage());
        }
        
        return metrics;
    }

    @GetMapping("/performance")
    public Map<String, Object> getPerformanceMetrics() {
        return calculatePerformanceMetrics();
    }

    @GetMapping("/sla")
    public Map<String, Object> getSLAMetrics() {
        return calculateSLAMetrics();
    }

    @GetMapping("/trends/tasks")
    public List<Map<String, Object>> getTaskTrends(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> trends = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            Date startDate = Date.from(date.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(date.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            
            long createdTasks = historyService.createHistoricTaskInstanceQuery()
                    .taskCreatedAfter(startDate)
                    .taskCreatedBefore(endDate)
                    .count();
                    
            long completedTasks = historyService.createHistoricTaskInstanceQuery()
                    .finished()
                    .taskCompletedAfter(startDate)
                    .taskCompletedBefore(endDate)
                    .count();
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toLocalDate().toString());
            dayData.put("created", createdTasks);
            dayData.put("completed", completedTasks);
            trends.add(dayData);
        }
        
        return trends;
    }

    @GetMapping("/trends/processes")
    public List<Map<String, Object>> getProcessTrends(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> trends = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            Date startDate = Date.from(date.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(date.toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            
            long startedProcesses = historyService.createHistoricProcessInstanceQuery()
                    .startedAfter(startDate)
                    .startedBefore(endDate)
                    .count();
                    
            long completedProcesses = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .finishedAfter(startDate)
                    .finishedBefore(endDate)
                    .count();
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toLocalDate().toString());
            dayData.put("started", startedProcesses);
            dayData.put("completed", completedProcesses);
            trends.add(dayData);
        }
        
        return trends;
    }

    @GetMapping("/realtime")
    public Map<String, Object> getRealtimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get current hour's data
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hourStart = now.withMinute(0).withSecond(0).withNano(0);
        Date startDate = Date.from(hourStart.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        
        long tasksCreatedThisHour = historyService.createHistoricTaskInstanceQuery()
                .taskCreatedAfter(startDate)
                .taskCreatedBefore(endDate)
                .count();
                
        long tasksCompletedThisHour = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskCompletedAfter(startDate)
                .taskCompletedBefore(endDate)
                .count();
                
        long processesStartedThisHour = historyService.createHistoricProcessInstanceQuery()
                .startedAfter(startDate)
                .startedBefore(endDate)
                .count();
        
        metrics.put("tasksCreated", tasksCreatedThisHour);
        metrics.put("tasksCompleted", tasksCompletedThisHour);
        metrics.put("processesStarted", processesStartedThisHour);
        metrics.put("timestamp", now.toString());
        
        return metrics;
    }

    // Scheduled method to push real-time updates
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Async
    public CompletableFuture<Void> pushDashboardUpdates() {
        try {
            Map<String, Object> dashboardData = getDashboardMetrics();
            notificationService.sendDashboardUpdate(dashboardData);
        } catch (Exception e) {
            System.err.println("Failed to push dashboard updates: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private Map<String, Object> calculateSLAMetrics() {
        Map<String, Object> slaMetrics = new HashMap<>();
        
        // Mock SLA calculations - implement real logic based on your requirements
        long totalTasks = taskService.createTaskQuery().count();
        
        // For demo purposes, generate realistic percentages
        Random random = new Random();
        int onTime = 70 + random.nextInt(20); // 70-90%
        int late = 5 + random.nextInt(10); // 5-15%
        int atRisk = 100 - onTime - late; // Remaining percentage
        
        slaMetrics.put("onTime", onTime);
        slaMetrics.put("late", late);
        slaMetrics.put("atRisk", atRisk);
        slaMetrics.put("totalTasks", totalTasks);
        
        return slaMetrics;
    }

    private Map<String, Object> calculatePerformanceMetrics() {
        Map<String, Object> performanceMetrics = new HashMap<>();
        
        // Calculate average task completion time
        long avgCompletionTime = calculateAverageProcessingTime();
        
        // Calculate throughput (tasks completed per hour)
        LocalDateTime now = LocalDateTime.now();
        Date oneHourAgo = Date.from(now.minusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        long tasksCompletedLastHour = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskCompletedAfter(oneHourAgo)
                .count();
        
        performanceMetrics.put("averageCompletionTime", avgCompletionTime);
        performanceMetrics.put("throughputPerHour", tasksCompletedLastHour);
        performanceMetrics.put("timestamp", now.toString());
        
        return performanceMetrics;
    }

    private long calculateAverageProcessingTime() {
        // Mock calculation - implement real logic based on your requirements
        // This would typically involve querying historic task instances and calculating duration
        return 45 + new Random().nextInt(60); // 45-105 minutes
    }

    private List<Map<String, Object>> getRecentActivities() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        try {
            // Get recent completed tasks
            var recentTasks = historyService.createHistoricTaskInstanceQuery()
                    .finished()
                    .orderByHistoricTaskInstanceEndTime()
                    .desc()
                    .listPage(0, 5);
            
            for (var task : recentTasks) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", task.getId());
                activity.put("type", "task_completed");
                activity.put("message", String.format("Task '%s' was completed", task.getName()));
                activity.put("timestamp", task.getEndTime());
                activity.put("user", task.getAssignee() != null ? task.getAssignee() : "System");
                activities.add(activity);
            }
            
            // Get recent started processes
            var recentProcesses = historyService.createHistoricProcessInstanceQuery()
                    .orderByProcessInstanceStartTime()
                    .desc()
                    .listPage(0, 5);
            
            for (var process : recentProcesses) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", process.getId());
                activity.put("type", "process_started");
                activity.put("message", String.format("Process '%s' was started", 
                    process.getProcessDefinitionName() != null ? process.getProcessDefinitionName() : process.getProcessDefinitionKey()));
                activity.put("timestamp", process.getStartTime());
                activity.put("user", process.getStartUserId() != null ? process.getStartUserId() : "System");
                activities.add(activity);
            }
            
            // Sort by timestamp desc
            activities.sort((a, b) -> {
                Date timestampA = (Date) a.get("timestamp");
                Date timestampB = (Date) b.get("timestamp");
                return timestampB.compareTo(timestampA);
            });
            
            // Return only top 10
            return activities.stream().limit(10).collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("Failed to get recent activities: " + e.getMessage());
            return activities;
        }
    }
}

