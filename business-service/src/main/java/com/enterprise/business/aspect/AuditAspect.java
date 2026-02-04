package com.enterprise.business.aspect;

import com.enterprise.business.entity.AuditLog;
import com.enterprise.business.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * Audit Aspect for automatic audit logging
 * Intercepts methods annotated with @Audited
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void logAudit(JoinPoint joinPoint, Audited audited, Object result) {
        try {
            // Get authentication info
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : "anonymous";
            
            // Get request info
            String ipAddress = getClientIp();
            String userAgent = getUserAgent();
            
            // Extract entity ID from result
            String entityId = extractEntityId(result, audited.entityIdField());
            
            // Create audit log
            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(audited.action())
                    .entityType(audited.entityType())
                    .entityId(entityId)
                    .newValue(toJson(result))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            auditLogRepository.save(auditLog);
            
            log.debug("Audit log created: {} {} by {}", audited.action(), audited.entityType(), username);
            
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
            // Don't throw exception to avoid breaking business logic
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractEntityId(Object result, String fieldName) {
        if (result == null || fieldName.isEmpty()) {
            return null;
        }
        
        try {
            Method getter = result.getClass().getMethod("get" + capitalize(fieldName));
            Object value = getter.invoke(result);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to extract entity ID from field: {}", fieldName);
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            // Simple JSON representation - can be enhanced with Jackson
            return obj.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
