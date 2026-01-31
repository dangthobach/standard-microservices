package com.enterprise.process.controller;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:3000")
public class DashboardController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ManagementService managementService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Running Instances
        long runningInstances = runtimeService.createProcessInstanceQuery().count();
        stats.put("runningInstances", runningInstances);

        // 2. Completed Instances (Last 24h)
        Date twentyFourHoursAgo = Date.from(Instant.now().minus(24, ChronoUnit.HOURS));
        long completedInstances24h = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .finishedAfter(twentyFourHoursAgo)
                .count();
        stats.put("completedInstances24h", completedInstances24h);

        // 3. Failed Jobs (Dead Letter Jobs)
        long failedJobs = managementService.createDeadLetterJobQuery().count();
        stats.put("failedJobs", failedJobs);

        // 4. Terminated (Simulated by checking for specific delete reason or state,
        // typically simply checking finished instances that were not naturally
        // completed is complex without proper reasons)
        // For now, let's just count total finished - completed naturally?
        // Flowable doesn't easily distinguish 'terminated' vs 'completed' without
        // checking deleteReason.
        // Let's check for deleteReason != null
        long terminatedInstances = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .deleted() // This usually implies cancelled/terminated
                .count();
        stats.put("terminatedInstances", terminatedInstances);

        // 5. System Health (Mock for now, but implies DB connectivity is fine if we got
        // here)
        stats.put("systemHealth", "OK");
        stats.put("uptime", "99.9%");

        // 6. Engine Load (Distribution by Definition Key - Top 5)
        // This is expensive to compute in real-time for large datasets, so we might
        // skip or cache it.
        // For this implementation, we will skip the detailed distribution or mock it
        // for the UI graph
        // until we have a proper metrics service.

        return ResponseEntity.ok(stats);
    }
}
