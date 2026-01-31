package com.enterprise.process.analytics;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:3000")
public class AnalyticsController {

    @Autowired
    private HistoryService historyService;

    // Optional: Autowire DmnHistoryService if available.
    // For now, we will simulate DMN stats or use basic logic.
    // @Autowired(required = false)
    // private DmnHistoryService dmnHistoryService;

    @GetMapping("/dmn-heatmap")
    public List<Map<String, Object>> getDmnHeatmap() {
        // In a real scenario, we would query DmnHistoryService
        // List<HistoricDecisionExecution> executions =
        // dmnHistoryService.createHistoricDecisionExecutionQuery().list();
        // and aggregate.

        // Mocking for now as we just enabled DMN and have no data
        List<Map<String, Object>> heatmap = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < 40; i++) {
            Map<String, Object> cell = new HashMap<>();
            cell.put("ruleId", "Rule-" + (100 + i));
            cell.put("hits", rand.nextInt(1000));
            heatmap.add(cell);
        }
        return heatmap;
    }

    @GetMapping("/bottlenecks")
    public List<Map<String, Object>> getBottlenecks() {
        Date twoHoursAgo = Date.from(Instant.now().minus(2, ChronoUnit.HOURS));

        // Find process instances running for more than 2 hours
        List<HistoricProcessInstance> slowInstances = historyService.createHistoricProcessInstanceQuery()
                .unfinished()
                .startedBefore(twoHoursAgo)
                .orderByProcessInstanceStartTime().asc()
                .listPage(0, 10);

        return slowInstances.stream().map(inst -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inst.getName() != null ? inst.getName() : inst.getId());
            map.put("node", "Unknown (Active)"); // Would need runtime query to get active activity

            long durationMs = Instant.now().toEpochMilli() - inst.getStartTime().getTime();
            long durationMin = durationMs / 60000;

            map.put("time", durationMin + " min");
            map.put("deviance", "+" + (durationMin - 120) + "m"); // Assuming 120m SLA
            map.put("color", "text-red-600");
            return map;
        }).collect(Collectors.toList());
    }
}
