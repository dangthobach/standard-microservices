import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Button, 
  Card, 
  Space, 
  message, 
  Modal, 
  Form, 
  Input, 
  Tag, 
  Typography,
  Row,
  Col,
  Descriptions,
  Divider
} from 'antd';
import { 
  PlayCircleOutlined, 
  StopOutlined, 
  EyeOutlined,
  UserOutlined,
  ClockCircleOutlined,
  FileTextOutlined
} from '@ant-design/icons';
import { cmmnApi } from '../services/flowableApi';
import { CaseDefinition, CaseInstance, PlanItemInstance } from '../types';

const { Title, Text } = Typography;

const CaseManagement: React.FC = () => {
  const [caseDefinitions, setCaseDefinitions] = useState<CaseDefinition[]>([]);
  const [caseInstances, setCaseInstances] = useState<CaseInstance[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedCase, setSelectedCase] = useState<CaseDefinition | null>(null);
  const [startModalVisible, setStartModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedInstance, setSelectedInstance] = useState<CaseInstance | null>(null);
  const [planItems, setPlanItems] = useState<PlanItemInstance[]>([]);
  const [startForm] = Form.useForm();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [definitions, instances] = await Promise.all([
        cmmnApi.getCaseDefinitions(),
        cmmnApi.getCaseInstances()
      ]);
      setCaseDefinitions(definitions);
      setCaseInstances(instances);
    } catch (error) {
      message.error('Failed to fetch case data');
      console.error('Error fetching case data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleStartCase = async (values: Record<string, any>) => {
    if (!selectedCase) return;
    
    try {
      await cmmnApi.startCase(selectedCase.key, values);
      message.success('Case started successfully');
      setStartModalVisible(false);
      startForm.resetFields();
      fetchData();
    } catch (error) {
      message.error('Failed to start case');
      console.error('Error starting case:', error);
    }
  };

  const handleTerminateCase = async (caseInstanceId: string) => {
    try {
      await cmmnApi.terminateCase(caseInstanceId);
      message.success('Case terminated successfully');
      fetchData();
    } catch (error) {
      message.error('Failed to terminate case');
      console.error('Error terminating case:', error);
    }
  };

  const handleViewCaseDetail = async (instance: CaseInstance) => {
    setSelectedInstance(instance);
    try {
      const items = await cmmnApi.getPlanItems(instance.id);
      setPlanItems(items);
      setDetailModalVisible(true);
    } catch (error) {
      message.error('Failed to fetch case details');
      console.error('Error fetching case details:', error);
    }
  };

  const getCaseStatusColor = (instance: CaseInstance) => {
    return 'blue'; // Active cases
  };

  const getPlanItemStatusColor = (item: PlanItemInstance) => {
    switch (item.state) {
      case 'active':
        return 'green';
      case 'completed':
        return 'blue';
      case 'terminated':
        return 'red';
      default:
        return 'orange';
    }
  };

  const caseDefinitionColumns = [
    {
      title: 'Case Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Key',
      dataIndex: 'key',
      key: 'key',
      render: (key: string) => <Text code>{key}</Text>,
    },
    {
      title: 'Version',
      dataIndex: 'version',
      key: 'version',
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: CaseDefinition) => (
        <Button
          type="primary"
          icon={<PlayCircleOutlined />}
          onClick={() => {
            setSelectedCase(record);
            setStartModalVisible(true);
          }}
        >
          Start Case
        </Button>
      ),
    },
  ];

  const caseInstanceColumns = [
    {
      title: 'Case Instance ID',
      dataIndex: 'id',
      key: 'id',
      render: (id: string) => <Text code>{id}</Text>,
    },
    {
      title: 'Case Definition',
      dataIndex: 'caseDefinitionId',
      key: 'caseDefinitionId',
      render: (id: string) => <Text code>{id}</Text>,
    },
    {
      title: 'Business Key',
      dataIndex: 'businessKey',
      key: 'businessKey',
    },
    {
      title: 'Start Time',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (time: string) => new Date(time).toLocaleString(),
    },
    {
      title: 'Status',
      key: 'status',
      render: (_: any, record: CaseInstance) => (
        <Tag color={getCaseStatusColor(record)}>Active</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: CaseInstance) => (
        <Space>
          <Button
            icon={<EyeOutlined />}
            onClick={() => handleViewCaseDetail(record)}
          >
            View
          </Button>
          <Button
            danger
            icon={<StopOutlined />}
            onClick={() => handleTerminateCase(record.id)}
          >
            Terminate
          </Button>
        </Space>
      ),
    },
  ];

  const planItemColumns = [
    {
      title: 'Plan Item',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Type',
      dataIndex: 'planItemDefinitionType',
      key: 'planItemDefinitionType',
    },
    {
      title: 'State',
      dataIndex: 'state',
      key: 'state',
      render: (state: string) => (
        <Tag color={getPlanItemStatusColor({ state } as PlanItemInstance)}>
          {state}
        </Tag>
      ),
    },
    {
      title: 'Start Time',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (time: string) => time ? new Date(time).toLocaleString() : '-',
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Title level={2}>
        <FileTextOutlined style={{ marginRight: '8px' }} />
        Case Management (CMMN)
      </Title>

      <Row gutter={24}>
        {/* Case Definitions */}
        <Col span={12}>
          <Card title="Case Definitions" style={{ marginBottom: '24px' }}>
            <Table
              columns={caseDefinitionColumns}
              dataSource={caseDefinitions}
              loading={loading}
              rowKey="id"
              pagination={{ pageSize: 5 }}
              size="small"
            />
          </Card>
        </Col>

        {/* Case Instances */}
        <Col span={12}>
          <Card title="Case Instances" style={{ marginBottom: '24px' }}>
            <Table
              columns={caseInstanceColumns}
              dataSource={caseInstances}
              loading={loading}
              rowKey="id"
              pagination={{ pageSize: 5 }}
              size="small"
            />
          </Card>
        </Col>
      </Row>

      {/* Start Case Modal */}
      <Modal
        title={`Start Case: ${selectedCase?.name}`}
        open={startModalVisible}
        onCancel={() => setStartModalVisible(false)}
        footer={null}
      >
        <Form form={startForm} onFinish={handleStartCase} layout="vertical">
          <Form.Item
            name="businessKey"
            label="Business Key"
            rules={[{ required: true, message: 'Please enter business key' }]}
          >
            <Input placeholder="Enter business key" />
          </Form.Item>
          <Form.Item
            name="assignee"
            label="Assignee"
            rules={[{ required: true, message: 'Please enter assignee' }]}
          >
            <Input placeholder="Enter assignee" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Start Case
              </Button>
              <Button onClick={() => setStartModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Case Detail Modal */}
      <Modal
        title="Case Instance Details"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        width={800}
        footer={null}
      >
        {selectedInstance && (
          <>
            <Descriptions title="Case Information" column={2} size="small">
              <Descriptions.Item label="Case Instance ID">
                <Text code>{selectedInstance.id}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Case Definition ID">
                <Text code>{selectedInstance.caseDefinitionId}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Business Key">
                {selectedInstance.businessKey || 'N/A'}
              </Descriptions.Item>
              <Descriptions.Item label="Start Time">
                <Tag icon={<ClockCircleOutlined />}>
                  {new Date(selectedInstance.startTime).toLocaleString()}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Start User">
                {selectedInstance.startUserId ? (
                  <Tag icon={<UserOutlined />}>{selectedInstance.startUserId}</Tag>
                ) : (
                  'N/A'
                )}
              </Descriptions.Item>
            </Descriptions>

            <Divider />

            <Title level={4}>Plan Items</Title>
            <Table
              columns={planItemColumns}
              dataSource={planItems}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </>
        )}
      </Modal>
    </div>
  );
};

export default CaseManagement;
