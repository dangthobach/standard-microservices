package com.enterprise.process.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Evaluates approval level based on business rules
 * 
 * Rules:
 * - BASIC: Price < 1000 AND category != HIGH_RISK
 * - STANDARD: Price < 10000 AND category != HIGH_RISK
 * - SENIOR: Price >= 10000 OR category == HIGH_RISK
 * - EXECUTIVE: Price >= 50000 OR category == CRITICAL
 */
@Component("approvalLevelEvaluator")
@Slf4j
public class ApprovalLevelEvaluator implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        // Get product details from process variables
        BigDecimal price = (BigDecimal) execution.getVariable("price");
        String category = (String) execution.getVariable("category");
        Integer stockQuantity = (Integer) execution.getVariable("stockQuantity");
        
        log.info("Evaluating approval level for product - Price: {}, Category: {}, Stock: {}", 
                price, category, stockQuantity);
        
        String approvalLevel = determineApprovalLevel(price, category, stockQuantity);
        
        // Set approval level in process variables
        execution.setVariable("approvalLevel", approvalLevel);
        
        // Set specific approval requirements
        switch (approvalLevel) {
            case "BASIC":
                execution.setVariable("requiresChecker", true);
                execution.setVariable("requiresConfirmer", false);
                execution.setVariable("requiresSeniorApproval", false);
                execution.setVariable("requiresExecutiveApproval", false);
                break;
            case "STANDARD":
                execution.setVariable("requiresChecker", true);
                execution.setVariable("requiresConfirmer", true);
                execution.setVariable("requiresSeniorApproval", false);
                execution.setVariable("requiresExecutiveApproval", false);
                break;
            case "SENIOR":
                execution.setVariable("requiresChecker", true);
                execution.setVariable("requiresConfirmer", true);
                execution.setVariable("requiresSeniorApproval", true);
                execution.setVariable("requiresExecutiveApproval", false);
                break;
            case "EXECUTIVE":
                execution.setVariable("requiresChecker", true);
                execution.setVariable("requiresConfirmer", true);
                execution.setVariable("requiresSeniorApproval", true);
                execution.setVariable("requiresExecutiveApproval", true);
                break;
        }
        
        log.info("Approval level determined: {} for product", approvalLevel);
    }

    private String determineApprovalLevel(BigDecimal price, String category, Integer stockQuantity) {
        // Executive approval for very high value or critical items
        if (price.compareTo(BigDecimal.valueOf(50000)) >= 0 || "CRITICAL".equals(category)) {
            return "EXECUTIVE";
        }
        
        // Senior approval for high value or high risk
        if (price.compareTo(BigDecimal.valueOf(10000)) >= 0 || "HIGH_RISK".equals(category)) {
            return "SENIOR";
        }
        
        // Standard approval for moderate value
        if (price.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return "STANDARD";
        }
        
        // Basic approval for low value items
        return "BASIC";
    }
}
