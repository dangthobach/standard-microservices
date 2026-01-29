package com.enterprise.process.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

@Controller
@CrossOrigin(origins = "http://localhost:3000")
public class WebSocketController {

    private final NotificationService notificationService;

    public WebSocketController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @MessageMapping("/dashboard/subscribe")
    @SendTo("/topic/dashboard")
    public Map<String, String> subscribeToDashboard() {
        return Map.of("status", "subscribed", "message", "Dashboard subscription confirmed");
    }

    @MessageMapping("/notifications/test")
    public void sendTestNotification() {
        notificationService.sendGlobalNotification("Test notification from WebSocket", "info");
    }

    @MessageMapping("/tasks/subscribe")
    @SendTo("/topic/tasks")
    public Map<String, String> subscribeToTasks() {
        return Map.of("status", "subscribed", "message", "Task updates subscription confirmed");
    }

    @MessageMapping("/processes/subscribe")
    @SendTo("/topic/processes")
    public Map<String, String> subscribeToProcesses() {
        return Map.of("status", "subscribed", "message", "Process updates subscription confirmed");
    }
}

