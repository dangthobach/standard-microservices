import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  Typography,
  Select,
  DatePicker,
  Table,
  Tag,
  Progress,
  Space,
  Button,
  message
} from 'antd';
import {
  BarChartOutlined,
  LineChartOutlined,
  PieChartOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { processApi } from '../services/flowableApi';
import { ProcessDefinition, ProcessInstance } from '../types';
import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';
import dayjs from 'dayjs';

const { Title } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

interface ProcessStats {
  total: number;
  running: number;
  completed: number;
  suspended: number;
  averageDuration: number;
  completionRate: number;
}

interface TimeSeriesData {
  date: string;
  started: number;
  completed: number;
  running: number;
}

interface ProcessPerformance {
  processKey: string;
  processName: string;
  totalInstances: number;
  completedInstances: number;
  averageDuration: number;
  completionRate: number;
  status: 'excellent' | 'good' | 'warning' | 'poor';
}

const Monitoring: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [processes, setProcesses] = useState<ProcessDefinition[]>([]);
  const [stats, setStats] = useState<ProcessStats>({
    total: 0,
    running: 0,
    completed: 0,
    suspended: 0,
    averageDuration: 0,
    completionRate: 0
  });
  const [timeSeriesData, setTimeSeriesData] = useState<TimeSeriesData[]>([]);
  const [processPerformance, setProcessPerformance] = useState<ProcessPerformance[]>([]);
  const [selectedProcess, setSelectedProcess] = useState<string>('all');
  const [dateRange, setDateRange] = useState<any>([
    dayjs().subtract(7, 'day'),
    dayjs()
  ]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [processesData, instancesData] = await Promise.all([
        processApi.getProcesses(),
        processApi.getProcessInstances()
      ]);
      
      setProcesses(processesData);
      
      // Calculate statistics
      calculateStats(instancesData);
      generateTimeSeriesData(instancesData);
      calculateProcessPerformance(instancesData, processesData);
      
    } catch (error) {
      message.error('Failed to fetch monitoring data');
      console.error('Error fetching monitoring data:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData, selectedProcess, dateRange]);

  const calculateStats = (instances: ProcessInstance[]) => {
    const total = instances.length;
    const running = instances.filter(i => !i.ended && !i.suspended).length;
    const completed = instances.filter(i => i.ended).length;
    const suspended = instances.filter(i => i.suspended).length;
    
    const completedInstances = instances.filter(i => i.ended && i.endTime);
    const totalDuration = completedInstances.reduce((sum, instance) => {
      const duration = dayjs(instance.endTime).diff(dayjs(instance.startTime), 'minute');
      return sum + duration;
    }, 0);
    
    const averageDuration = completedInstances.length > 0 ? totalDuration / completedInstances.length : 0;
    const completionRate = total > 0 ? (completed / total) * 100 : 0;

    setStats({
      total,
      running,
      completed,
      suspended,
      averageDuration,
      completionRate
    });
  };

  const generateTimeSeriesData = (instances: ProcessInstance[]) => {
    const data: { [key: string]: TimeSeriesData } = {};
    
    // Initialize data for the last 7 days
    for (let i = 6; i >= 0; i--) {
      const date = dayjs().subtract(i, 'day').format('YYYY-MM-DD');
      data[date] = {
        date,
        started: 0,
        completed: 0,
        running: 0
      };
    }

    instances.forEach(instance => {
      const startDate = dayjs(instance.startTime).format('YYYY-MM-DD');
      const endDate = instance.endTime ? dayjs(instance.endTime).format('YYYY-MM-DD') : null;
      
      if (data[startDate]) {
        data[startDate].started++;
      }
      
      if (endDate && data[endDate]) {
        data[endDate].completed++;
      }
      
      if (!instance.ended && !instance.suspended) {
        const today = dayjs().format('YYYY-MM-DD');
        if (data[today]) {
          data[today].running++;
        }
      }
    });

    setTimeSeriesData(Object.values(data));
  };

  const calculateProcessPerformance = (instances: ProcessInstance[], processes: ProcessDefinition[]) => {
    const performanceData: ProcessPerformance[] = [];

    processes.forEach(process => {
      const processInstances = instances.filter(i => i.processDefinitionKey === process.key);
      const total = processInstances.length;
      const completed = processInstances.filter(i => i.ended).length;
      
      const completedInstances = processInstances.filter(i => i.ended && i.endTime);
      const totalDuration = completedInstances.reduce((sum, instance) => {
        return sum + dayjs(instance.endTime).diff(dayjs(instance.startTime), 'minute');
      }, 0);
      
      const averageDuration = completedInstances.length > 0 ? totalDuration / completedInstances.length : 0;
      const completionRate = total > 0 ? (completed / total) * 100 : 0;
      
      let status: 'excellent' | 'good' | 'warning' | 'poor' = 'good';
      if (completionRate >= 90) status = 'excellent';
      else if (completionRate >= 70) status = 'good';
      else if (completionRate >= 50) status = 'warning';
      else status = 'poor';

      performanceData.push({
        processKey: process.key,
        processName: process.name,
        totalInstances: total,
        completedInstances: completed,
        averageDuration,
        completionRate,
        status
      });
    });

    setProcessPerformance(performanceData);
  };

  const pieData = [
    { name: 'Running', value: stats.running, color: '#52c41a' },
    { name: 'Completed', value: stats.completed, color: '#1890ff' },
    { name: 'Suspended', value: stats.suspended, color: '#fa8c16' }
  ];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'excellent': return 'green';
      case 'good': return 'blue';
      case 'warning': return 'orange';
      case 'poor': return 'red';
      default: return 'default';
    }
  };

  const performanceColumns = [
    {
      title: 'Process Name',
      dataIndex: 'processName',
      key: 'processName',
      render: (name: string) => <strong>{name}</strong>
    },
    {
      title: 'Total Instances',
      dataIndex: 'totalInstances',
      key: 'totalInstances',
      sorter: (a: ProcessPerformance, b: ProcessPerformance) => a.totalInstances - b.totalInstances
    },
    {
      title: 'Completed',
      dataIndex: 'completedInstances',
      key: 'completedInstances',
      sorter: (a: ProcessPerformance, b: ProcessPerformance) => a.completedInstances - b.completedInstances
    },
    {
      title: 'Completion Rate',
      dataIndex: 'completionRate',
      key: 'completionRate',
      render: (rate: number) => (
        <Progress
          percent={Math.round(rate)}
          size="small"
          status={rate >= 70 ? 'success' : rate >= 50 ? 'normal' : 'exception'}
        />
      ),
      sorter: (a: ProcessPerformance, b: ProcessPerformance) => a.completionRate - b.completionRate
    },
    {
      title: 'Avg Duration (min)',
      dataIndex: 'averageDuration',
      key: 'averageDuration',
      render: (duration: number) => Math.round(duration),
      sorter: (a: ProcessPerformance, b: ProcessPerformance) => a.averageDuration - b.averageDuration
    },
    {
      title: 'Performance',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>
          {status.toUpperCase()}
        </Tag>
      ),
      filters: [
        { text: 'Excellent', value: 'excellent' },
        { text: 'Good', value: 'good' },
        { text: 'Warning', value: 'warning' },
        { text: 'Poor', value: 'poor' }
      ],
      onFilter: (value: any, record: ProcessPerformance) => record.status === value
    }
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Row justify="space-between" align="middle" style={{ marginBottom: '24px' }}>
        <Col>
          <Title level={2}>
            <BarChartOutlined style={{ marginRight: '8px' }} />
            Process Monitoring Dashboard
          </Title>
        </Col>
        <Col>
          <Space>
            <Select
              value={selectedProcess}
              onChange={setSelectedProcess}
              style={{ width: 200 }}
            >
              <Option value="all">All Processes</Option>
              {processes.map(process => (
                <Option key={process.key} value={process.key}>
                  {process.name}
                </Option>
              ))}
            </Select>
            <RangePicker
              value={dateRange}
              onChange={setDateRange}
            />
            <Button
              icon={<ReloadOutlined />}
              onClick={fetchData}
              loading={loading}
            >
              Refresh
            </Button>
          </Space>
        </Col>
      </Row>

      {/* Overview Statistics */}
      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Total Processes"
              value={stats.total}
              prefix={<PlayCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Running"
              value={stats.running}
              prefix={<PlayCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Completed"
              value={stats.completed}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Suspended"
              value={stats.suspended}
              prefix={<PauseCircleOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Avg Duration (min)"
              value={Math.round(stats.averageDuration)}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Completion Rate"
              value={Math.round(stats.completionRate)}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: stats.completionRate >= 70 ? '#52c41a' : '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Charts */}
      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col span={16}>
          <Card title={<><LineChartOutlined /> Process Activity Timeline</>}>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={timeSeriesData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="started" stroke="#8884d8" name="Started" />
                <Line type="monotone" dataKey="completed" stroke="#82ca9d" name="Completed" />
                <Line type="monotone" dataKey="running" stroke="#ffc658" name="Running" />
              </LineChart>
            </ResponsiveContainer>
          </Card>
        </Col>
        <Col span={8}>
          <Card title={<><PieChartOutlined /> Process Status Distribution</>}>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }: any) => `${name} ${((percent || 0) * 100).toFixed(0)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {pieData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      {/* Process Performance Table */}
      <Card title="Process Performance Analysis">
        <Table
          columns={performanceColumns}
          dataSource={processPerformance}
          loading={loading}
          rowKey="processKey"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} of ${total} processes`,
          }}
        />
      </Card>
    </div>
  );
};

export default Monitoring;
