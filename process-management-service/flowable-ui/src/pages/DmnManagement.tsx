import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  message,
  Modal,
  Form,
  Input,
  Typography,
  Row,
  Col,
  Tag,
  Popconfirm,
  Tabs,
  Drawer,
  Select,
  InputNumber,
  Divider
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  TableOutlined,
  CodeOutlined
} from '@ant-design/icons';
import { DecisionDefinition } from '../types';
import { decisionApi } from '../services/flowableApi';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;
const { TabPane } = Tabs;

interface DecisionRule {
  id: string;
  inputs: Record<string, any>;
  outputs: Record<string, any>;
  description?: string;
}

interface DecisionTable {
  key: string;
  name: string;
  description: string;
  inputVariables: string[];
  outputVariables: string[];
  rules: DecisionRule[];
}

interface TestCase {
  name: string;
  inputs: Record<string, any>;
  expectedOutputs: Record<string, any>;
}

const DmnManagement: React.FC = () => {
  const [decisions, setDecisions] = useState<DecisionDefinition[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [designerVisible, setDesignerVisible] = useState(false);
  const [testVisible, setTestVisible] = useState(false);
  const [selectedDecision, setSelectedDecision] = useState<DecisionTable | null>(null);
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [testResults, setTestResults] = useState<any[]>([]);
  const [form] = Form.useForm();
  const [testForm] = Form.useForm();

  useEffect(() => {
    fetchDecisions();
  }, []);

  const fetchDecisions = async () => {
    setLoading(true);
    try {
      const data = await decisionApi.getDecisions();
      setDecisions(data);
    } catch (error) {
      message.error('Failed to fetch decisions');
      console.error('Error fetching decisions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateDecision = () => {
    form.resetFields();
    setModalVisible(true);
  };

  const handleEditDecision = (decision: DecisionDefinition) => {
    // Load decision definition
    const decisionTable: DecisionTable = {
      key: decision.key,
      name: decision.name,
      description: 'Decision table description',
      inputVariables: ['customerType', 'orderAmount'],
      outputVariables: ['discount', 'shippingMethod'],
      rules: [
        {
          id: '1',
          inputs: { customerType: 'PREMIUM', orderAmount: '>= 100' },
          outputs: { discount: 0.15, shippingMethod: 'EXPRESS' },
          description: 'Premium customers with large orders get 15% discount'
        },
        {
          id: '2',
          inputs: { customerType: 'PREMIUM', orderAmount: '< 100' },
          outputs: { discount: 0.10, shippingMethod: 'STANDARD' },
          description: 'Premium customers get 10% discount'
        },
        {
          id: '3',
          inputs: { customerType: 'REGULAR', orderAmount: '>= 50' },
          outputs: { discount: 0.05, shippingMethod: 'STANDARD' },
          description: 'Regular customers with medium orders get 5% discount'
        },
        {
          id: '4',
          inputs: { customerType: 'REGULAR', orderAmount: '< 50' },
          outputs: { discount: 0.0, shippingMethod: 'ECONOMY' },
          description: 'Regular customers with small orders get no discount'
        }
      ]
    };
    
    setSelectedDecision(decisionTable);
    setDesignerVisible(true);
  };

  const handleDeleteDecision = async (decisionId: string) => {
    try {
      await decisionApi.deleteDecision(decisionId);
      message.success('Decision deleted successfully');
      fetchDecisions();
    } catch (error) {
      message.error('Failed to delete decision');
      console.error('Error deleting decision:', error);
    }
  };

  const handleTestDecision = (decision: DecisionDefinition) => {
    const testData: TestCase[] = [
      {
        name: 'Premium Customer Large Order',
        inputs: { customerType: 'PREMIUM', orderAmount: 150 },
        expectedOutputs: { discount: 0.15, shippingMethod: 'EXPRESS' }
      },
      {
        name: 'Regular Customer Small Order',
        inputs: { customerType: 'REGULAR', orderAmount: 30 },
        expectedOutputs: { discount: 0.0, shippingMethod: 'ECONOMY' }
      }
    ];
    
    setTestCases(testData);
    setSelectedDecision({
      key: decision.key,
      name: decision.name,
      description: '',
      inputVariables: ['customerType', 'orderAmount'],
      outputVariables: ['discount', 'shippingMethod'],
      rules: []
    });
    setTestVisible(true);
  };

  const handleSaveDecision = async (values: any) => {
    try {
      // TODO: Implement API call to save decision
      console.log('Saving decision:', values);
      message.success('Decision saved successfully');
      setModalVisible(false);
      fetchDecisions();
    } catch (error) {
      message.error('Failed to save decision');
      console.error('Error saving decision:', error);
    }
  };

  const executeTest = async (testCase: TestCase) => {
    try {
      if (!selectedDecision) return;
      
      const result = await decisionApi.evaluateDecision(selectedDecision.key, testCase.inputs);
      
      setTestResults(prev => [
        ...prev,
        {
          testName: testCase.name,
          inputs: testCase.inputs,
          expectedOutputs: testCase.expectedOutputs,
          actualOutputs: result,
          passed: JSON.stringify(result) === JSON.stringify(testCase.expectedOutputs)
        }
      ]);
      
      message.success(`Test "${testCase.name}" executed successfully`);
    } catch (error) {
      message.error(`Test "${testCase.name}" failed`);
      console.error('Error executing test:', error);
    }
  };

  const executeAllTests = async () => {
    setTestResults([]);
    for (const testCase of testCases) {
      await executeTest(testCase);
    }
  };

  const addRule = () => {
    if (!selectedDecision) return;
    
    const newRule: DecisionRule = {
      id: Date.now().toString(),
      inputs: {},
      outputs: {},
      description: ''
    };
    
    setSelectedDecision({
      ...selectedDecision,
      rules: [...selectedDecision.rules, newRule]
    });
  };

  const removeRule = (ruleId: string) => {
    if (!selectedDecision) return;
    
    const filteredRules = selectedDecision.rules.filter(rule => rule.id !== ruleId);
    setSelectedDecision({
      ...selectedDecision,
      rules: filteredRules
    });
  };

  const columns = [
    {
      title: 'Decision Name',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <Text strong>{name}</Text>
    },
    {
      title: 'Decision Key',
      dataIndex: 'key',
      key: 'key',
      render: (key: string) => <Text code>{key}</Text>
    },
    {
      title: 'Version',
      dataIndex: 'version',
      key: 'version',
      render: (version: number) => <Tag color="blue">v{version}</Tag>
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      render: (category: string) => category || 'General'
    },
    {
      title: 'Resource Name',
      dataIndex: 'resourceName',
      key: 'resourceName',
      render: (name: string) => <Text type="secondary">{name}</Text>
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: DecisionDefinition) => (
        <Space>
          <Button
            icon={<PlayCircleOutlined />}
            onClick={() => handleTestDecision(record)}
            type="primary"
            size="small"
          >
            Test
          </Button>
          
          <Button
            icon={<TableOutlined />}
            onClick={() => handleEditDecision(record)}
            size="small"
          >
            Designer
          </Button>
          
          <Button
            icon={<EditOutlined />}
            onClick={() => handleEditDecision(record)}
            size="small"
          >
            Edit
          </Button>
          
          <Popconfirm
            title="Are you sure you want to delete this decision?"
            onConfirm={() => handleDeleteDecision(record.id)}
            okText="Yes"
            cancelText="No"
          >
            <Button
              icon={<DeleteOutlined />}
              danger
              size="small"
            >
              Delete
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  const testColumns = [
    {
      title: 'Test Name',
      dataIndex: 'testName',
      key: 'testName'
    },
    {
      title: 'Status',
      dataIndex: 'passed',
      key: 'passed',
      render: (passed: boolean) => (
        <Tag color={passed ? 'green' : 'red'}>
          {passed ? 'PASSED' : 'FAILED'}
        </Tag>
      )
    },
    {
      title: 'Inputs',
      dataIndex: 'inputs',
      key: 'inputs',
      render: (inputs: Record<string, any>) => (
        <pre style={{ fontSize: '12px', margin: 0 }}>
          {JSON.stringify(inputs, null, 2)}
        </pre>
      )
    },
    {
      title: 'Expected Outputs',
      dataIndex: 'expectedOutputs',
      key: 'expectedOutputs',
      render: (outputs: Record<string, any>) => (
        <pre style={{ fontSize: '12px', margin: 0 }}>
          {JSON.stringify(outputs, null, 2)}
        </pre>
      )
    },
    {
      title: 'Actual Outputs',
      dataIndex: 'actualOutputs',
      key: 'actualOutputs',
      render: (outputs: Record<string, any>) => (
        <pre style={{ fontSize: '12px', margin: 0 }}>
          {JSON.stringify(outputs, null, 2)}
        </pre>
      )
    }
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Row justify="space-between" align="middle" style={{ marginBottom: '24px' }}>
        <Col>
          <Title level={2}>
            <TableOutlined style={{ marginRight: '8px' }} />
            DMN Decision Management
          </Title>
        </Col>
        <Col>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreateDecision}
            size="large"
          >
            Create New Decision
          </Button>
        </Col>
      </Row>

      <Card>
        <Table
          columns={columns}
          dataSource={decisions}
          loading={loading}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} of ${total} decisions`
          }}
        />
      </Card>

      {/* Create/Edit Decision Modal */}
      <Modal
        title="Create New Decision"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        width={600}
        footer={[
          <Button key="cancel" onClick={() => setModalVisible(false)}>
            Cancel
          </Button>,
          <Button key="save" type="primary" onClick={() => form.submit()}>
            Save Decision
          </Button>
        ]}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveDecision}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="Decision Key"
                name="decisionKey"
                rules={[{ required: true, message: 'Please enter decision key' }]}
              >
                <Input placeholder="Enter unique decision key" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Decision Name"
                name="decisionName"
                rules={[{ required: true, message: 'Please enter decision name' }]}
              >
                <Input placeholder="Enter decision name" />
              </Form.Item>
            </Col>
          </Row>
          
          <Form.Item
            label="Description"
            name="description"
          >
            <TextArea rows={3} placeholder="Enter decision description" />
          </Form.Item>

          <Form.Item
            label="Category"
            name="category"
          >
            <Select placeholder="Select category">
              <Option value="business-rules">Business Rules</Option>
              <Option value="scoring">Scoring</Option>
              <Option value="routing">Routing</Option>
              <Option value="validation">Validation</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Decision Designer Drawer */}
      <Drawer
        title={`Decision Designer: ${selectedDecision?.name}`}
        open={designerVisible}
        onClose={() => setDesignerVisible(false)}
        width="90%"
        extra={
          <Space>
            <Button onClick={addRule} type="primary" icon={<PlusOutlined />}>
              Add Rule
            </Button>
            <Button icon={<CodeOutlined />}>
              View DMN XML
            </Button>
          </Space>
        }
      >
        {selectedDecision && (
          <Tabs defaultActiveKey="table">
            <TabPane tab="Decision Table" key="table">
              <Card>
                <Row gutter={16} style={{ marginBottom: '16px' }}>
                  <Col span={12}>
                    <Text strong>Input Variables: </Text>
                    {selectedDecision.inputVariables.join(', ')}
                  </Col>
                  <Col span={12}>
                    <Text strong>Output Variables: </Text>
                    {selectedDecision.outputVariables.join(', ')}
                  </Col>
                </Row>
                
                <Divider />
                
                <Table
                  dataSource={selectedDecision.rules}
                  rowKey="id"
                  pagination={false}
                  size="small"
                >
                  <Table.Column
                    title="Rule ID"
                    dataIndex="id"
                    width={80}
                  />
                  {selectedDecision.inputVariables.map(variable => (
                    <Table.Column
                      key={variable}
                      title={variable}
                      dataIndex={['inputs', variable]}
                      render={(value) => <Text code>{value || '-'}</Text>}
                    />
                  ))}
                  {selectedDecision.outputVariables.map(variable => (
                    <Table.Column
                      key={variable}
                      title={variable}
                      dataIndex={['outputs', variable]}
                      render={(value) => <Text strong>{value || '-'}</Text>}
                    />
                  ))}
                  <Table.Column
                    title="Description"
                    dataIndex="description"
                    render={(desc) => <Text type="secondary">{desc || '-'}</Text>}
                  />
                  <Table.Column
                    title="Actions"
                    render={(_, record: DecisionRule) => (
                      <Button
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => removeRule(record.id)}
                      >
                        Delete
                      </Button>
                    )}
                  />
                </Table>
              </Card>
            </TabPane>
            
            <TabPane tab="DMN XML" key="xml">
              <div style={{ background: '#f5f5f5', padding: '16px', borderRadius: '4px' }}>
                <pre>
                  <code>{`<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/DMN/20180521/MODEL/">
  <decision id="${selectedDecision.key}" name="${selectedDecision.name}">
    <decisionTable id="DecisionTable_${selectedDecision.key}">
      <!-- Decision table rules would be generated here -->
    </decisionTable>
  </decision>
</definitions>`}</code>
                </pre>
              </div>
            </TabPane>
          </Tabs>
        )}
      </Drawer>

      {/* Test Decision Modal */}
      <Modal
        title={`Test Decision: ${selectedDecision?.name}`}
        open={testVisible}
        onCancel={() => setTestVisible(false)}
        width={1000}
        footer={[
          <Button key="close" onClick={() => setTestVisible(false)}>
            Close
          </Button>,
          <Button key="run-all" type="primary" onClick={executeAllTests}>
            Run All Tests
          </Button>
        ]}
      >
        <Tabs defaultActiveKey="predefined">
          <TabPane tab="Predefined Tests" key="predefined">
            <Space direction="vertical" style={{ width: '100%' }}>
              {testCases.map((testCase, index) => (
                <Card key={index} size="small" title={testCase.name}>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Text strong>Inputs:</Text>
                      <pre style={{ fontSize: '12px' }}>
                        {JSON.stringify(testCase.inputs, null, 2)}
                      </pre>
                    </Col>
                    <Col span={8}>
                      <Text strong>Expected Outputs:</Text>
                      <pre style={{ fontSize: '12px' }}>
                        {JSON.stringify(testCase.expectedOutputs, null, 2)}
                      </pre>
                    </Col>
                    <Col span={8}>
                      <Button
                        type="primary"
                        onClick={() => executeTest(testCase)}
                        style={{ marginTop: '8px' }}
                      >
                        Run Test
                      </Button>
                    </Col>
                  </Row>
                </Card>
              ))}
            </Space>
          </TabPane>
          
          <TabPane tab="Custom Test" key="custom">
            <Form
              form={testForm}
              layout="vertical"
              onFinish={(values) => {
                const customTest: TestCase = {
                  name: 'Custom Test',
                  inputs: values,
                  expectedOutputs: {}
                };
                executeTest(customTest);
              }}
            >
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item label="Customer Type" name="customerType">
                    <Select>
                      <Option value="PREMIUM">Premium</Option>
                      <Option value="REGULAR">Regular</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item label="Order Amount" name="orderAmount">
                    <InputNumber style={{ width: '100%' }} min={0} />
                  </Form.Item>
                </Col>
              </Row>
              <Button type="primary" htmlType="submit">
                Execute Custom Test
              </Button>
            </Form>
          </TabPane>
          
          <TabPane tab="Results" key="results">
            <Table
              columns={testColumns}
              dataSource={testResults}
              rowKey="testName"
              pagination={false}
            />
          </TabPane>
        </Tabs>
      </Modal>
    </div>
  );
};

export default DmnManagement;
