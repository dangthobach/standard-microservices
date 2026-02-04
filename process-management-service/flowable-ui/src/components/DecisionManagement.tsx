import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Button, 
  Card, 
  Space, 
  message, 
  Modal, 
  Form, 
  Tag, 
  Typography,
  Row,
  Col,
  Descriptions,
  Divider,
  InputNumber,
  Select
} from 'antd';
import { 
  EyeOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  ExperimentOutlined
} from '@ant-design/icons';
import { decisionApi } from '../services/flowableApi';
import { DecisionDefinition, DecisionTable, DecisionExecution } from '../types';

const { Title, Text } = Typography;
const { Option } = Select;

const DecisionManagement: React.FC = () => {
  const [decisionDefinitions, setDecisionDefinitions] = useState<DecisionDefinition[]>([]);
  const [decisionTables, setDecisionTables] = useState<DecisionTable[]>([]);
  const [decisionHistory, setDecisionHistory] = useState<DecisionExecution[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedDecision, setSelectedDecision] = useState<DecisionDefinition | null>(null);
  const [executeModalVisible, setExecuteModalVisible] = useState(false);
  const [historyModalVisible, setHistoryModalVisible] = useState(false);
  const [selectedExecution, setSelectedExecution] = useState<DecisionExecution | null>(null);
  const [executeForm] = Form.useForm();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [definitions, tables, history] = await Promise.all([
        decisionApi.getDecisions(),
        decisionApi.getDecisionTables(),
        decisionApi.getDecisionHistory()
      ]);
      setDecisionDefinitions(definitions);
      setDecisionTables(tables);
      setDecisionHistory(history);
    } catch (error) {
      message.error('Failed to fetch decision data');
      console.error('Error fetching decision data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleExecuteDecision = async (values: Record<string, any>) => {
    if (!selectedDecision) return;
    
    try {
      const result = await decisionApi.evaluateDecision(selectedDecision.key, values);
      message.success('Decision executed successfully');
      setExecuteModalVisible(false);
      executeForm.resetFields();
      fetchData();
      
      // Show result in a new modal
      Modal.info({
        title: 'Decision Result',
        content: (
          <div>
            <p><strong>Decision Key:</strong> {result.decisionKey}</p>
            <p><strong>Result:</strong></p>
            <pre>{JSON.stringify(result.result, null, 2)}</pre>
          </div>
        ),
        width: 600,
      });
    } catch (error) {
      message.error('Failed to execute decision');
      console.error('Error executing decision:', error);
    }
  };

  const handleViewExecutionDetail = async (execution: DecisionExecution) => {
    try {
      const detail = await decisionApi.getDecisionExecution(execution.id);
      setSelectedExecution(detail);
      setHistoryModalVisible(true);
    } catch (error) {
      message.error('Failed to fetch execution details');
      console.error('Error fetching execution details:', error);
    }
  };

  const decisionDefinitionColumns = [
    {
      title: 'Decision Name',
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
      render: (_: any, record: DecisionDefinition) => (
        <Button
          type="primary"
          icon={<ExperimentOutlined />}
          onClick={() => {
            setSelectedDecision(record);
            setExecuteModalVisible(true);
          }}
        >
          Execute
        </Button>
      ),
    },
  ];

  const decisionTableColumns = [
    {
      title: 'Table Name',
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
  ];

  const decisionHistoryColumns = [
    {
      title: 'Execution ID',
      dataIndex: 'id',
      key: 'id',
      render: (id: string) => <Text code>{id}</Text>,
    },
    {
      title: 'Decision Key',
      dataIndex: 'decisionKey',
      key: 'decisionKey',
      render: (key: string) => <Text code>{key}</Text>,
    },
    {
      title: 'Decision Name',
      dataIndex: 'decisionName',
      key: 'decisionName',
    },
    {
      title: 'Execution Time',
      dataIndex: 'executionTime',
      key: 'executionTime',
      render: (time: string) => new Date(time).toLocaleString(),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DecisionExecution) => (
        <Button
          icon={<EyeOutlined />}
          onClick={() => handleViewExecutionDetail(record)}
        >
          View
        </Button>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Title level={2}>
        <FileTextOutlined style={{ marginRight: '8px' }} />
        Decision Management (DMN)
      </Title>

      <Row gutter={24}>
        {/* Decision Definitions */}
        <Col span={8}>
          <Card title="Decision Definitions" style={{ marginBottom: '24px' }}>
            <Table
              columns={decisionDefinitionColumns}
              dataSource={decisionDefinitions}
              loading={loading}
              rowKey="id"
              pagination={{ pageSize: 5 }}
              size="small"
            />
          </Card>
        </Col>

        {/* Decision Tables */}
        <Col span={8}>
          <Card title="Decision Tables" style={{ marginBottom: '24px' }}>
            <Table
              columns={decisionTableColumns}
              dataSource={decisionTables}
              loading={loading}
              rowKey="id"
              pagination={{ pageSize: 5 }}
              size="small"
            />
          </Card>
        </Col>

        {/* Decision History */}
        <Col span={8}>
          <Card title="Decision History" style={{ marginBottom: '24px' }}>
            <Table
              columns={decisionHistoryColumns}
              dataSource={decisionHistory}
              loading={loading}
              rowKey="id"
              pagination={{ pageSize: 5 }}
              size="small"
            />
          </Card>
        </Col>
      </Row>

      {/* Execute Decision Modal */}
      <Modal
        title={`Execute Decision: ${selectedDecision?.name}`}
        open={executeModalVisible}
        onCancel={() => setExecuteModalVisible(false)}
        footer={null}
      >
        <Form form={executeForm} onFinish={handleExecuteDecision} layout="vertical">
          <Form.Item
            name="routeType"
            label="Route Type"
            rules={[{ required: true, message: 'Please select route type' }]}
          >
            <Select placeholder="Select route type">
              <Option value="fast">Fast</Option>
              <Option value="normal">Normal</Option>
              <Option value="slow">Slow</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="priority"
            label="Priority"
            rules={[{ required: true, message: 'Please enter priority' }]}
          >
            <InputNumber 
              min={1} 
              max={10} 
              placeholder="Enter priority (1-10)" 
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<ExperimentOutlined />}>
                Execute Decision
              </Button>
              <Button onClick={() => setExecuteModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Execution Detail Modal */}
      <Modal
        title="Decision Execution Details"
        open={historyModalVisible}
        onCancel={() => setHistoryModalVisible(false)}
        width={800}
        footer={null}
      >
        {selectedExecution && (
          <>
            <Descriptions title="Execution Information" column={2} size="small">
              <Descriptions.Item label="Execution ID">
                <Text code>{selectedExecution.id}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Decision Key">
                <Text code>{selectedExecution.decisionKey}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Decision Name">
                {selectedExecution.decisionName}
              </Descriptions.Item>
              <Descriptions.Item label="Instance ID">
                {selectedExecution.instanceId || 'N/A'}
              </Descriptions.Item>
              <Descriptions.Item label="Execution Time">
                <Tag icon={<ClockCircleOutlined />}>
                  {new Date(selectedExecution.executionTime).toLocaleString()}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="End Time">
                {selectedExecution.endTime ? (
                  <Tag>{new Date(selectedExecution.endTime).toLocaleString()}</Tag>
                ) : (
                  'N/A'
                )}
              </Descriptions.Item>
            </Descriptions>

            <Divider />

            <Row gutter={24}>
              <Col span={12}>
                <Title level={4}>Input Variables</Title>
                <pre style={{ background: '#f5f5f5', padding: '12px', borderRadius: '4px' }}>
                  {JSON.stringify(selectedExecution.inputVariables || {}, null, 2)}
                </pre>
              </Col>
              <Col span={12}>
                <Title level={4}>Output Variables</Title>
                <pre style={{ background: '#f5f5f5', padding: '12px', borderRadius: '4px' }}>
                  {JSON.stringify(selectedExecution.outputVariables || {}, null, 2)}
                </pre>
              </Col>
            </Row>
          </>
        )}
      </Modal>
    </div>
  );
};

export default DecisionManagement;
