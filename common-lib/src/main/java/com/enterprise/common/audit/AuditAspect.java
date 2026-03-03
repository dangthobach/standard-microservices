package com.enterprise.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

/**
 * Aspect to intercept @Audited methods and publish AuditEvents.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditAspect {

    private final AuditEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final SensitiveDataMasker masker;

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            try {
                captureAudit(joinPoint, audited, result, errorMessage, System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("Failed to capture audit log", e);
            }
        }
    }

    private void captureAudit(ProceedingJoinPoint joinPoint, Audited audited, Object result, String errorMessage,
            long duration) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String action = audited.action().isEmpty() ? signature.getMethod().getName() : audited.action();
        String entityType = audited.entityType();

        // Get User
        String username = "system";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            username = authentication.getName();
        }

        // Get IP and User Agent
        String ipAddress = null;
        String userAgent = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
        }

        // Build Event
        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .username(username)
                .action(action)
                .entityType(entityType)
                // .entityId() // Difficult to extract generically without convention
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(Instant.now())
                .build();

        // Populate specific fields if possible (e.g., from arguments)
        // This is a naive implementation; production would filter args for IDs

        publisher.publish(event);
    }
}
