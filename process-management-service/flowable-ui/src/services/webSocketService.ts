import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface NotificationMessage {
  message: string;
  type: string;
  timestamp: string;
  user?: string;
  icon?: React.ReactNode;
}

export interface TaskNotification {
  taskId: string;
  taskName: string;
  processName: string;
  type: string;
  timestamp: string;
}

export interface ProcessNotification {
  processInstanceId: string;
  processName: string;
  status: string;
  timestamp: string;
}

export interface DashboardData {
  totalTasks: number;
  completedTasks: number;
  activeProcesses: number;
  completedProcesses: number;
  averageProcessingTime: number;
  tasksByStatus: Record<string, number>;
  processesByStatus: Record<string, number>;
  recentActivities: any[];
  performanceMetrics: any;
}

export type NotificationCallback = (notification: NotificationMessage) => void;
export type TaskNotificationCallback = (notification: TaskNotification) => void;
export type ProcessNotificationCallback = (notification: ProcessNotification) => void;
export type DashboardUpdateCallback = (data: DashboardData) => void;

class WebSocketService {
  private client: Client | null = null;
  private connected = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;

  // Callbacks
  private notificationCallbacks: NotificationCallback[] = [];
  private taskNotificationCallbacks: TaskNotificationCallback[] = [];
  private processNotificationCallbacks: ProcessNotificationCallback[] = [];
  private dashboardUpdateCallbacks: DashboardUpdateCallback[] = [];

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.client = new Client({
          webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
          connectHeaders: {},
          debug: (str) => {
            console.log('STOMP Debug:', str);
          },
          reconnectDelay: this.reconnectDelay,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
        });

        this.client.onConnect = () => {
          console.log('WebSocket connected');
          this.connected = true;
          this.reconnectAttempts = 0;
          
          this.subscribeToChannels();
          resolve();
        };

        this.client.onStompError = (frame) => {
          console.error('WebSocket error:', frame);
          reject(new Error(`WebSocket error: ${frame.headers['message']}`));
        };

        this.client.onWebSocketError = (error) => {
          console.error('WebSocket connection error:', error);
          this.handleReconnect();
        };

        this.client.onDisconnect = () => {
          console.log('WebSocket disconnected');
          this.connected = false;
          this.handleReconnect();
        };

        this.client.activate();

      } catch (error) {
        console.error('Error connecting to WebSocket:', error);
        reject(error);
      }
    });
  }

  private subscribeToChannels() {
    if (!this.client || !this.connected) return;

    // Subscribe to global notifications
    this.client.subscribe('/topic/notifications', (message) => {
      const notification: NotificationMessage = JSON.parse(message.body);
      this.notificationCallbacks.forEach(callback => callback(notification));
    });

    // Subscribe to task notifications
    this.client.subscribe('/topic/tasks', (message) => {
      const notification: TaskNotification = JSON.parse(message.body);
      this.taskNotificationCallbacks.forEach(callback => callback(notification));
    });

    // Subscribe to process notifications
    this.client.subscribe('/topic/processes', (message) => {
      const notification: ProcessNotification = JSON.parse(message.body);
      this.processNotificationCallbacks.forEach(callback => callback(notification));
    });

    // Subscribe to dashboard updates
    this.client.subscribe('/topic/dashboard', (message) => {
      const data: DashboardData = JSON.parse(message.body);
      this.dashboardUpdateCallbacks.forEach(callback => callback(data));
    });

    // Send subscription confirmations
    this.client.publish({
      destination: '/app/dashboard/subscribe',
      body: JSON.stringify({ action: 'subscribe' })
    });
  }

  private handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        this.connect().catch(error => {
          console.error('Reconnection failed:', error);
        });
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }

  disconnect() {
    if (this.client && this.connected) {
      this.client.deactivate();
      this.connected = false;
    }
  }

  isConnected(): boolean {
    return this.connected;
  }

  // Subscription methods
  onNotification(callback: NotificationCallback) {
    this.notificationCallbacks.push(callback);
    return () => {
      const index = this.notificationCallbacks.indexOf(callback);
      if (index > -1) {
        this.notificationCallbacks.splice(index, 1);
      }
    };
  }

  onTaskNotification(callback: TaskNotificationCallback) {
    this.taskNotificationCallbacks.push(callback);
    return () => {
      const index = this.taskNotificationCallbacks.indexOf(callback);
      if (index > -1) {
        this.taskNotificationCallbacks.splice(index, 1);
      }
    };
  }

  onProcessNotification(callback: ProcessNotificationCallback) {
    this.processNotificationCallbacks.push(callback);
    return () => {
      const index = this.processNotificationCallbacks.indexOf(callback);
      if (index > -1) {
        this.processNotificationCallbacks.splice(index, 1);
      }
    };
  }

  onDashboardUpdate(callback: DashboardUpdateCallback) {
    this.dashboardUpdateCallbacks.push(callback);
    return () => {
      const index = this.dashboardUpdateCallbacks.indexOf(callback);
      if (index > -1) {
        this.dashboardUpdateCallbacks.splice(index, 1);
      }
    };
  }

  // Send messages
  sendTestNotification() {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/notifications/test',
        body: JSON.stringify({ message: 'Test notification' })
      });
    }
  }

  subscribeToTasks() {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/tasks/subscribe',
        body: JSON.stringify({ action: 'subscribe' })
      });
    }
  }

  subscribeToProcesses() {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/processes/subscribe',
        body: JSON.stringify({ action: 'subscribe' })
      });
    }
  }
}

// Create singleton instance
const webSocketService = new WebSocketService();

export default webSocketService;
