import React, { useState, useEffect, useCallback } from 'react';
import { 
  Card, 
  Row, 
  Col, 
  Statistic, 
  Typography, 
  List, 
  Tag, 
  Space, 
  Button,
  Select,
  notification,
  Badge,
  Avatar,
  Progress
} from 'antd';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import {
  DashboardOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  PlayCircleOutlined,
  UserOutlined,
  RiseOutlined,
  NotificationOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { taskApi, processApi } from '../services/flowableApi';
import webSocketService, { DashboardData, NotificationMessage } from '../services/webSocketService';
import dayjs from '../utils/dayjs';
import { Task, ProcessInstance, DashboardMetrics } from '../types';

const { Title } = Typography;
const { Option } = Select;

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<DashboardMetrics>({
    totalTasks: 0,
    completedTasks: 0,
    pendingTasks: 0,
    activeProcesses: 0,
    completedProcesses: 0,
    averageProcessingTime: 0,
    tasksByStatus: {},
    processesByStatus: {},
    slaMetrics: { onTime: 0, late: 0, atRisk: 0 }
  });
  const [tasks, setTasks] = useState<Task[]>([]);
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
  const [chartData, setChartData] = useState<any[]>([]);
  const [timeRange, setTimeRange] = useState('7d');
  
  // Real-time data refresh
  const refreshData = useCallback(async () => {
    setLoading(true);
    try {
      // Fetch all data in parallel
      const [tasksResponse, processesResponse] = await Promise.all([
        taskApi.getTasks(),
        processApi.getProcessInstances()
      ]);

      const tasksData = Array.isArray(tasksResponse) ? tasksResponse : [];
      const processesData = Array.isArray(processesResponse) ? processesResponse : [];

      setTasks(tasksData);

      // Calculate metrics
      const totalTasks = tasksData.length;
      const completedTasks = tasksData.filter((task: Task) => !task.id).length;
      const pendingTasks = totalTasks - completedTasks;
      const activeProcesses = processesData.filter((p: ProcessInstance) => p.state === 'active' || !p.suspended).length;
      const completedProcesses = processesData.length - activeProcesses;

      // Group tasks by status
      const tasksByStatus = tasksData.reduce((acc: Record<string, number>, task: Task) => {
        const status = task.assignee ? 'assigned' : 'unassigned';
        acc[status] = (acc[status] || 0) + 1;
        return acc;
      }, {});

      // Group processes by status  
      const processesByStatus = processesData.reduce((acc: Record<string, number>, process: ProcessInstance) => {
        const status = process.ended ? 'completed' : 'active';
        acc[status] = (acc[status] || 0) + 1;
        return acc;
      }, {});

      // SLA metrics calculation
      const now = dayjs();
      const slaMetrics = {
        onTime: tasksData.filter((task: Task) => !task.dueDate || dayjs(task.dueDate).isAfter(now)).length,
        late: tasksData.filter((task: Task) => task.dueDate && dayjs(task.dueDate).isBefore(now)).length,
        atRisk: tasksData.filter((task: Task) => task.dueDate && dayjs(task.dueDate).diff(now, 'hours') <= 24 && dayjs(task.dueDate).isAfter(now)).length,
      };

      const newStats: DashboardMetrics = {
        totalTasks,
        completedTasks,
        pendingTasks,
        activeProcesses,
        completedProcesses,
        averageProcessingTime: 0,
        tasksByStatus,
        processesByStatus,
        slaMetrics
      };

      setStats(newStats);

      // Generate chart data
      const last7Days = Array.from({ length: 7 }, (_, i) => {
        const date = dayjs().subtract(i, 'day');
        return {
          date: date.format('MM/DD'),
          tasks: Math.floor(Math.random() * 20) + 10,
          completed: Math.floor(Math.random() * 15) + 5,
          processes: Math.floor(Math.random() * 10) + 3,
        };
      }).reverse();
      
      setChartData(last7Days);

    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      notification.error({
        message: 'Error',
        description: 'Failed to fetch dashboard data'
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshData();

    // Subscribe to WebSocket updates
    const handleDashboardUpdate = (data: DashboardData) => {
      setStats(prev => ({ ...prev, ...data }));
    };

    const handleNotification = (notification: NotificationMessage) => {
      setNotifications(prev => [notification, ...prev.slice(0, 9)]);
    };

    webSocketService.onDashboardUpdate(handleDashboardUpdate);
    webSocketService.onNotification(handleNotification);

    // Auto-refresh every 30 seconds
    const interval = setInterval(refreshData, 30000);

    return () => {
      clearInterval(interval);
      webSocketService.disconnect();
    };
  }, [refreshData]);

  const handleTimeRangeChange = (value: string) => {
    setTimeRange(value);
    refreshData();
  };

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8'];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2}>
          <DashboardOutlined /> Dashboard
        </Title>
        <Space>
          <Select value={timeRange} onChange={handleTimeRangeChange} style={{ width: 120 }}>
            <Option value="1d">Last Day</Option>
            <Option value="7d">Last Week</Option>
            <Option value="30d">Last Month</Option>
          </Select>
          <Button icon={<ReloadOutlined />} onClick={refreshData} loading={loading}>
            Refresh
          </Button>
        </Space>
      </div>

      {/* Key Metrics */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Total Tasks"
              value={stats.totalTasks}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Completed Tasks"
              value={stats.completedTasks}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Active Processes"
              value={stats.activeProcesses}
              prefix={<PlayCircleOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Completion Rate"
              value={Math.round((stats.completedTasks / stats.totalTasks) * 100) || 0}
              valueStyle={{ color: '#722ed1' }}
              prefix={<RiseOutlined />}
              suffix="%"
            />
          </Card>
        </Col>
      </Row>

      {/* SLA Metrics */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} lg={12}>
          <Card title="SLA Performance" extra={<Badge status="processing" text="Real-time" />}>
            <Row gutter={16}>
              <Col span={8}>
                <Statistic
                  title="On Time"
                  value={stats.slaMetrics.onTime}
                  valueStyle={{ color: '#52c41a' }}
                />
              </Col>
              <Col span={8}>
                <Statistic
                  title="At Risk"
                  value={stats.slaMetrics.atRisk}
                  valueStyle={{ color: '#fa8c16' }}
                />
              </Col>
              <Col span={8}>
                <Statistic
                  title="Overdue"
                  value={stats.slaMetrics.late}
                  valueStyle={{ color: '#ff4d4f' }}
                />
              </Col>
            </Row>
            <div style={{ marginTop: 16 }}>
              <Progress
                percent={Math.round((stats.slaMetrics.onTime / (stats.slaMetrics.onTime + stats.slaMetrics.late + stats.slaMetrics.atRisk)) * 100) || 0}
                status="active"
                strokeColor="#52c41a"
              />
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Process Status Distribution">
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie
                  data={Object.entries(stats.processesByStatus).map(([key, value]) => ({
                    name: key,
                    value: value
                  }))}
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                  label
                >
                  {Object.entries(stats.processesByStatus).map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <RechartsTooltip />
              </PieChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      {/* Charts */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} lg={12}>
          <Card title="Task Completion Trend" extra={<Badge status="processing" text="Live" />}>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <RechartsTooltip />
                <Area type="monotone" dataKey="completed" stackId="1" stroke="#52c41a" fill="#52c41a" fillOpacity={0.6} />
                <Area type="monotone" dataKey="tasks" stackId="1" stroke="#1890ff" fill="#1890ff" fillOpacity={0.6} />
              </AreaChart>
            </ResponsiveContainer>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Process Activity">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <RechartsTooltip />
                <Bar dataKey="processes" fill="#fa8c16" />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      {/* Recent Activity */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card 
            title="Recent Tasks" 
            extra={
              <Badge count={tasks.length} style={{ backgroundColor: '#52c41a' }}>
                <NotificationOutlined />
              </Badge>
            }
          >
            <List
              dataSource={tasks.slice(0, 5)}
              renderItem={(task) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={<Avatar icon={<UserOutlined />} />}
                    title={task.name}
                    description={
                      <Space>
                        <Tag color={task.assignee ? 'blue' : 'orange'}>
                          {task.assignee || 'Unassigned'}
                        </Tag>
                        <span>{dayjs(task.created).format('MM/DD HH:mm')}</span>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card 
            title="Live Notifications" 
            extra={
              <Badge count={notifications.length} style={{ backgroundColor: '#1890ff' }}>
                <NotificationOutlined />
              </Badge>
            }
          >
            <List
              dataSource={notifications.slice(0, 5)}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={item.icon}
                    title={
                      <Space>
                        {item.message}
                        <Tag>{item.user}</Tag>
                      </Space>
                    }
                    description={dayjs(item.timestamp).format('YYYY-MM-DD HH:mm:ss')}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
