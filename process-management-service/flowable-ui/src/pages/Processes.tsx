import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  message,
  Tag,
  Typography,
  Row,
  Col,
  Input,
  Select,
  DatePicker,
  Modal,
  Descriptions
} from 'antd';
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  EyeOutlined,
  SearchOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  UserOutlined
} from '@ant-design/icons';
import { processApi } from '../services/flowableApi';
import { ProcessInstance } from '../types';
import ProcessDiagram from '../components/ProcessDiagram';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const Processes: React.FC = () => {
  const [processInstances, setProcessInstances] = useState<ProcessInstance[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedInstance, setSelectedInstance] = useState<ProcessInstance | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [diagramVisible, setDiagramVisible] = useState(false);
  const [currentBpmn, setCurrentBpmn] = useState<string>('');
  const [filters, setFilters] = useState({
    processDefinitionKey: '',
    state: '',
    startedBy: '',
    dateRange: null as any
  });

  useEffect(() => {
    fetchProcessInstances();
  }, []);

  const fetchProcessInstances = async () => {
    setLoading(true);
    try {
      const data = await processApi.getProcessInstances();
      setProcessInstances(data);
    } catch (error) {
      message.error('Failed to fetch process instances');
      console.error('Error fetching process instances:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSuspendProcess = async (processInstanceId: string) => {
    try {
      await processApi.suspendProcessInstance(processInstanceId);
      message.success('Process suspended successfully');
      fetchProcessInstances();
    } catch (error) {
      message.error('Failed to suspend process');
      console.error('Error suspending process:', error);
    }
  };

  const handleActivateProcess = async (processInstanceId: string) => {
    try {
      await processApi.activateProcessInstance(processInstanceId);
      message.success('Process activated successfully');
      fetchProcessInstances();
    } catch (error) {
      message.error('Failed to activate process');
      console.error('Error activating process:', error);
    }
  };

  const handleDeleteProcess = async (processInstanceId: string) => {
    try {
      await processApi.deleteProcessInstance(processInstanceId, 'Deleted by user');
      message.success('Process deleted successfully');
      fetchProcessInstances();
    } catch (error) {
      message.error('Failed to delete process');
      console.error('Error deleting process:', error);
    }
  };

  const handleViewDiagram = async (instance: ProcessInstance) => {
    try {
      const bpmnXml = await processApi.getProcessBpmn(instance.processDefinitionKey);
      setSelectedInstance(instance);
      setCurrentBpmn(bpmnXml);
      setDiagramVisible(true);
    } catch (error) {
      message.error('Failed to load process diagram');
      console.error('Error loading diagram:', error);
    }
  };

  const handleViewDetails = (instance: ProcessInstance) => {
    setSelectedInstance(instance);
    setDetailVisible(true);
  };

  const getStatusColor = (suspended: boolean, ended: boolean) => {
    if (ended) return 'red';
    if (suspended) return 'orange';
    return 'green';
  };

  const getStatusText = (suspended: boolean, ended: boolean) => {
    if (ended) return 'Completed';
    if (suspended) return 'Suspended';
    return 'Running';
  };

  const columns = [
    {
      title: 'Process Name',
      dataIndex: 'processDefinitionName',
      key: 'processDefinitionName',
      render: (name: string) => <Text strong>{name}</Text>,
    },
    {
      title: 'Process Key',
      dataIndex: 'processDefinitionKey',
      key: 'processDefinitionKey',
      render: (key: string) => <Text code>{key}</Text>,
    },
    {
      title: 'Instance ID',
      dataIndex: 'id',
      key: 'id',
      render: (id: string) => <Text code>{id.substring(0, 8)}...</Text>,
    },
    {
      title: 'Status',
      key: 'status',
      render: (_: any, record: ProcessInstance) => (
        <Tag color={getStatusColor(record.suspended, record.ended)}>
          {getStatusText(record.suspended, record.ended)}
        </Tag>
      ),
    },
    {
      title: 'Started By',
      dataIndex: 'startUserId',
      key: 'startUserId',
      render: (userId: string) => (
        <span>
          <UserOutlined style={{ marginRight: 4 }} />
          {userId || 'System'}
        </span>
      ),
    },
    {
      title: 'Start Time',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (time: string) => (
        <span>
          <ClockCircleOutlined style={{ marginRight: 4 }} />
          {dayjs(time).format('YYYY-MM-DD HH:mm:ss')}
        </span>
      ),
    },
    {
      title: 'Duration',
      key: 'duration',
      render: (_: any, record: ProcessInstance) => {
        if (record.ended && record.endTime) {
          const duration = dayjs(record.endTime).diff(dayjs(record.startTime), 'minute');
          return `${duration} min`;
        }
        const duration = dayjs().diff(dayjs(record.startTime), 'minute');
        return `${duration} min (running)`;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: ProcessInstance) => (
        <Space>
          <Button
            icon={<EyeOutlined />}
            onClick={() => handleViewDetails(record)}
            size="small"
          >
            Details
          </Button>
          
          <Button
            icon={<EyeOutlined />}
            onClick={() => handleViewDiagram(record)}
            size="small"
          >
            Diagram
          </Button>
          
          {!record.ended && (
            <>
              {record.suspended ? (
                <Button
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleActivateProcess(record.id)}
                  type="primary"
                  size="small"
                >
                  Activate
                </Button>
              ) : (
                <Button
                  icon={<PauseCircleOutlined />}
                  onClick={() => handleSuspendProcess(record.id)}
                  size="small"
                >
                  Suspend
                </Button>
              )}
              
              <Button
                icon={<StopOutlined />}
                onClick={() => handleDeleteProcess(record.id)}
                danger
                size="small"
              >
                Delete
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Row justify="space-between" align="middle" style={{ marginBottom: '24px' }}>
        <Col>
          <Title level={2}>
            <PlayCircleOutlined style={{ marginRight: '8px' }} />
            Process Instances
          </Title>
        </Col>
        <Col>
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchProcessInstances}
            loading={loading}
          >
            Refresh
          </Button>
        </Col>
      </Row>

      {/* Filters */}
      <Card style={{ marginBottom: '16px' }}>
        <Row gutter={16}>
          <Col span={6}>
            <Input
              placeholder="Search by process key"
              prefix={<SearchOutlined />}
              value={filters.processDefinitionKey}
              onChange={(e) => setFilters({ ...filters, processDefinitionKey: e.target.value })}
            />
          </Col>
          <Col span={6}>
            <Select
              placeholder="Filter by status"
              style={{ width: '100%' }}
              value={filters.state}
              onChange={(value) => setFilters({ ...filters, state: value })}
              allowClear
            >
              <Option value="active">Running</Option>
              <Option value="suspended">Suspended</Option>
              <Option value="completed">Completed</Option>
            </Select>
          </Col>
          <Col span={6}>
            <Input
              placeholder="Started by user"
              value={filters.startedBy}
              onChange={(e) => setFilters({ ...filters, startedBy: e.target.value })}
            />
          </Col>
          <Col span={6}>
            <RangePicker
              style={{ width: '100%' }}
              value={filters.dateRange}
              onChange={(dates) => setFilters({ ...filters, dateRange: dates })}
            />
          </Col>
        </Row>
      </Card>

      <Card>
        <Table
          columns={columns}
          dataSource={processInstances}
          loading={loading}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} of ${total} process instances`,
          }}
        />
      </Card>

      {/* Process Details Modal */}
      <Modal
        title={`Process Instance Details: ${selectedInstance?.id}`}
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>
            Close
          </Button>
        ]}
        width={800}
      >
        {selectedInstance && (
          <Descriptions column={2} bordered>
            <Descriptions.Item label="Process Name">
              {selectedInstance.processDefinitionName}
            </Descriptions.Item>
            <Descriptions.Item label="Process Key">
              {selectedInstance.processDefinitionKey}
            </Descriptions.Item>
            <Descriptions.Item label="Instance ID">
              {selectedInstance.id}
            </Descriptions.Item>
            <Descriptions.Item label="Business Key">
              {selectedInstance.businessKey || 'N/A'}
            </Descriptions.Item>
            <Descriptions.Item label="Started By">
              {selectedInstance.startUserId || 'System'}
            </Descriptions.Item>
            <Descriptions.Item label="Start Time">
              {dayjs(selectedInstance.startTime).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
            <Descriptions.Item label="End Time">
              {selectedInstance.endTime ? 
                dayjs(selectedInstance.endTime).format('YYYY-MM-DD HH:mm:ss') : 
                'Still running'
              }
            </Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color={getStatusColor(selectedInstance.suspended, selectedInstance.ended)}>
                {getStatusText(selectedInstance.suspended, selectedInstance.ended)}
              </Tag>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* Process Diagram Modal */}
      <Modal
        title={`Process Diagram: ${selectedInstance?.processDefinitionName}`}
        open={diagramVisible}
        onCancel={() => setDiagramVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDiagramVisible(false)}>
            Close
          </Button>
        ]}
        width="90%"
        style={{ top: 20 }}
        destroyOnClose
      >
        {currentBpmn && (
          <ProcessDiagram
            bpmnXml={currentBpmn}
            currentActivity={selectedInstance?.id}
          />
        )}
      </Modal>
    </div>
  );
};

export default Processes;
