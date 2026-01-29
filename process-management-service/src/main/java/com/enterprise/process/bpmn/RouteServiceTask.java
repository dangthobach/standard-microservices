package com.enterprise.process.bpmn;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteServiceTask implements JavaDelegate {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteServiceTask.class);
    
    @Override
    public void execute(DelegateExecution execution) {
        logger.info("Executing RouteServiceTask for process instance: {}", execution.getProcessInstanceId());
        
        // Simple routing logic - you can customize this based on your business requirements
        String processVariable = (String) execution.getVariable("routeType");
        
        if (processVariable == null) {
            processVariable = "default";
        }
        
        // Set a variable that can be used for routing in the process
        execution.setVariable("routingResult", processVariable);
        
        logger.info("Routing result set to: {}", processVariable);
    }
}

