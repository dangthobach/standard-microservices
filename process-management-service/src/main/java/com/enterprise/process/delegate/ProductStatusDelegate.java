package com.enterprise.process.delegate;

import com.enterprise.process.producer.ProductStatusProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.flowable.common.engine.api.delegate.Expression;

/**
 * JavaDelegate to update Product Status.
 * Called by Service Tasks in the BPMN process.
 */
@Slf4j
@Component("productStatusDelegate")
@RequiredArgsConstructor
public class ProductStatusDelegate implements JavaDelegate {

    private final ProductStatusProducer productStatusProducer;

    // Field injection from BPMN (e.g., "PENDING_APPROVAL")
    private Expression status;

    @Override
    public void execute(DelegateExecution execution) {
        String newStatus = (String) status.getValue(execution);
        String businessKey = execution.getProcessInstanceBusinessKey(); // This should be productId
        String processInstanceId = execution.getProcessInstanceId();

        log.info("ProductStatusDelegate executing: productId={}, newStatus={}", businessKey, newStatus);

        // Send event to Business Service
        productStatusProducer.sendStatusChange(businessKey, newStatus, processInstanceId);
    }
}
