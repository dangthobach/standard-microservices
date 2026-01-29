package com.enterprise.process.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = {"http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000"})
public class ProcessController {

    @GetMapping("/process-definitions")
    public ResponseEntity<List<Map<String, Object>>> getProcessDefinitions() {
        // Mock data - in a real implementation, you would integrate with Flowable
        List<Map<String, Object>> processes = List.of(
            Map.of(
                "id", "1",
                "key", "employee-review-process",
                "name", "Employee Review Process",
                "version", 1,
                "description", "Annual employee performance review process"
            ),
            Map.of(
                "id", "2", 
                "key", "customer-onboarding-process",
                "name", "Customer Onboarding Process",
                "version", 1,
                "description", "New customer registration and onboarding workflow"
            ),
            Map.of(
                "id", "3",
                "key", "expense-approval-process", 
                "name", "Expense Approval Process",
                "version", 1,
                "description", "Employee expense request and approval workflow"
            ),
            Map.of(
                "id", "4",
                "key", "leave-request-process",
                "name", "Leave Request Process", 
                "version", 1,
                "description", "Employee leave request and approval workflow"
            ),
            Map.of(
                "id", "5",
                "key", "purchase-order-process",
                "name", "Purchase Order Process",
                "version", 1,
                "description", "Purchase order creation and approval workflow"
            )
        );
        
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/process-definitions/{key}")
    public ResponseEntity<Map<String, Object>> getProcessDefinitionByKey(@PathVariable String key) {
        // Mock implementation - find process by key
        List<Map<String, Object>> processes = List.of(
            Map.of("id", "1", "key", "employee-review-process", "name", "Employee Review Process"),
            Map.of("id", "2", "key", "customer-onboarding-process", "name", "Customer Onboarding Process"),
            Map.of("id", "3", "key", "expense-approval-process", "name", "Expense Approval Process")
        );
        
        return processes.stream()
            .filter(process -> process.get("key").equals(key))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

