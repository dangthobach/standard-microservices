package com.enterprise.process.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Send notification to all users
    public void sendGlobalNotification(String message, String type) {
        NotificationMessage notification = new NotificationMessage(message, type, LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    // Send notification to specific user
    public void sendUserNotification(String userId, String message, String type) {
        NotificationMessage notification = new NotificationMessage(message, type, LocalDateTime.now());
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
    }

    // Send task assignment notification
    public void sendTaskAssignedNotification(String userId, String taskId, String taskName, String processName) {
        TaskNotification notification = new TaskNotification(
            taskId, 
            taskName, 
            processName, 
            "TASK_ASSIGNED", 
            LocalDateTime.now()
        );
        messagingTemplate.convertAndSendToUser(userId, "/queue/tasks", notification);
    }

    // Send process status change notification
    public void sendProcessStatusNotification(String processInstanceId, String processName, String status) {
        ProcessNotification notification = new ProcessNotification(
            processInstanceId,
            processName,
            status,
            LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/processes", notification);
    }

    // Send dashboard update
    public void sendDashboardUpdate(Map<String, Object> dashboardData) {
        messagingTemplate.convertAndSend("/topic/dashboard", dashboardData);
    }

    // Notification message classes
    public static class NotificationMessage {
        private String message;
        private String type;
        private LocalDateTime timestamp;

        public NotificationMessage(String message, String type, LocalDateTime timestamp) {
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class TaskNotification {
        private String taskId;
        private String taskName;
        private String processName;
        private String type;
        private LocalDateTime timestamp;

        public TaskNotification(String taskId, String taskName, String processName, String type, LocalDateTime timestamp) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.processName = processName;
            this.type = type;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class ProcessNotification {
        private String processInstanceId;
        private String processName;
        private String status;
        private LocalDateTime timestamp;

        public ProcessNotification(String processInstanceId, String processName, String status, LocalDateTime timestamp) {
            this.processInstanceId = processInstanceId;
            this.processName = processName;
            this.status = status;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getProcessInstanceId() { return processInstanceId; }
        public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}

