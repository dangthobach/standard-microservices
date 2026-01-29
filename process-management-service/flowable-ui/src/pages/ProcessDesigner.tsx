import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  message, 
  Typography,
  Row,
  Col,
  Tag,
  Popconfirm,
  Layout,
  Tooltip,
  Divider,
  Modal,
  Steps
} from 'antd';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  PlayCircleOutlined,
  EyeOutlined,
  DownloadOutlined,
  FileTextOutlined,
  ArrowLeftOutlined,
  SaveOutlined,
  UndoOutlined,
  RedoOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  ExpandOutlined,
  SettingOutlined
} from '@ant-design/icons';
import { processApi } from '../services/flowableApi';
import { ProcessDefinition } from '../types';
import BpmnEditor from '../components/BpmnEditor';

const { Title, Text } = Typography;
const { Sider, Content } = Layout;

const ProcessDesigner: React.FC = () => {
  const [processes, setProcesses] = useState<ProcessDefinition[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedProcess, setSelectedProcess] = useState<ProcessDefinition | null>(null);
  const [viewerVisible, setViewerVisible] = useState(false);
  const [currentBpmn, setCurrentBpmn] = useState<string>('');
  const [designerMode, setDesignerMode] = useState(false); // New state for full page mode
  const [viewerMode, setViewerMode] = useState(false); // New state for full page viewer
  const [processInstance, setProcessInstance] = useState<any>(null);
  const [taskHistory, setTaskHistory] = useState<any[]>([]);
  const [processVariables, setProcessVariables] = useState<any[]>([]);

  useEffect(() => {
    fetchProcesses();
  }, []);

  const fetchProcesses = async () => {
    setLoading(true);
    try {
      const data = await processApi.getProcesses();
      setProcesses(data);
    } catch (error) {
      message.error('Failed to fetch processes');
      console.error('Error fetching processes:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateNew = () => {
    setSelectedProcess(null);
    setCurrentBpmn('');
    setDesignerMode(true);
  };

  const handleEdit = async (process: ProcessDefinition) => {
    try {
      // Fetch BPMN XML for editing
      const bpmnXml = await processApi.getProcessBpmn(process.key);
      setSelectedProcess(process);
      setCurrentBpmn(bpmnXml);
      setDesignerMode(true);
    } catch (error) {
      message.error('Failed to load process for editing');
      console.error('Error loading process:', error);
    }
  };

  const handleView = async (process: ProcessDefinition) => {
    try {
      setLoading(true);
      // Fetch BPMN XML for viewing
      const bpmnXml = await processApi.getProcessBpmn(process.key);
      
      // Fetch process instances for history
      const instances = await processApi.getProcessInstances(process.key);
      let latestInstance = null;
      let history: any[] = [];
      let variables: any[] = [];
      
      if (instances.length > 0) {
        latestInstance = instances[0];
        // Fetch task history for the latest instance
        try {
          history = await processApi.getTaskHistory(latestInstance.id);
        } catch (error) {
          console.log('No task history available');
        }
        
        // Fetch process variables
        try {
          variables = await processApi.getProcessVariables(latestInstance.id);
        } catch (error) {
          console.log('No variables available');
        }
      }
      
      setSelectedProcess(process);
      setCurrentBpmn(bpmnXml);
      setProcessInstance(latestInstance);
      setTaskHistory(history);
      setProcessVariables(variables);
      setViewerMode(true);
      setLoading(false);
    } catch (error) {
      message.error('Failed to load process for viewing');
      console.error('Error loading process:', error);
      setLoading(false);
    }
  };

  const handleSave = async (xml: string, processKey: string, processName: string) => {
    try {
      // Deploy the process
      await processApi.deployProcess(xml, processKey, processName);
      message.success('Process deployed successfully');
      setDesignerMode(false);
      fetchProcesses(); // Refresh the list
    } catch (error) {
      message.error('Failed to deploy process');
      console.error('Error deploying process:', error);
      throw error;
    }
  };

  const handleUpdateVariable = async (variableName: string, newValue: any) => {
    if (!processInstance) return;
    
    try {
      await processApi.updateProcessVariable(processInstance.id, variableName, newValue);
      message.success('Variable updated successfully');
      
      // Refresh variables
      const updatedVariables = await processApi.getProcessVariables(processInstance.id);
      setProcessVariables(updatedVariables);
    } catch (error) {
      message.error('Failed to update variable');
      console.error('Error updating variable:', error);
    }
  };

  const handleBackToList = () => {
    setDesignerMode(false);
    setViewerMode(false);
    setSelectedProcess(null);
    setCurrentBpmn('');
    setProcessInstance(null);
    setTaskHistory([]);
    setProcessVariables([]);
  };

  const handleDelete = async (processId: string) => {
    try {
      await processApi.deleteProcess(processId);
      message.success('Process deleted successfully');
      fetchProcesses();
    } catch (error) {
      message.error('Failed to delete process');
      console.error('Error deleting process:', error);
    }
  };

  const handleStartProcess = async (processKey: string) => {
    try {
      await processApi.startProcess(processKey, {});
      message.success(`Process ${processKey} started successfully`);
    } catch (error) {
      message.error('Failed to start process');
      console.error('Error starting process:', error);
    }
  };

  const handleExportBpmn = async (process: ProcessDefinition) => {
    try {
      const bpmnXml = await processApi.getProcessBpmn(process.key);
      
      // Create and download file
      const blob = new Blob([bpmnXml], { type: 'application/xml' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${process.key}.bpmn`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      
      message.success('BPMN file exported successfully');
    } catch (error) {
      message.error('Failed to export BPMN file');
      console.error('Error exporting BPMN:', error);
    }
  };

  const columns = [
    {
      title: 'Process Name',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <Text strong>{name}</Text>,
    },
    {
      title: 'Process Key',
      dataIndex: 'key',
      key: 'key',
      render: (key: string) => <Text code>{key}</Text>,
    },
    {
      title: 'Version',
      dataIndex: 'version',
      key: 'version',
      render: (version: number) => <Tag color="blue">v{version}</Tag>,
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      render: (category: string) => category || 'General',
    },
    {
      title: 'Status',
      dataIndex: 'suspended',
      key: 'suspended',
      render: (suspended: boolean) => (
        <Tag color={suspended ? 'red' : 'green'}>
          {suspended ? 'Suspended' : 'Active'}
        </Tag>
      ),
    },
    {
      title: 'Deployment ID',
      dataIndex: 'deploymentId',
      key: 'deploymentId',
      render: (id: string) => <Text code>{id?.substring(0, 8)}...</Text>,
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: ProcessDefinition) => (
        <Space>
          <Button
            icon={<EyeOutlined />}
            onClick={() => handleView(record)}
            size="small"
          >
            View
          </Button>
          
          <Button
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            type="primary"
            size="small"
          >
            Edit
          </Button>
          
          <Button
            icon={<PlayCircleOutlined />}
            onClick={() => handleStartProcess(record.key)}
            type="default"
            size="small"
          >
            Start
          </Button>
          
          <Button
            icon={<DownloadOutlined />}
            onClick={() => handleExportBpmn(record)}
            size="small"
          >
            Export
          </Button>
          
          <Popconfirm
            title="Are you sure you want to delete this process?"
            onConfirm={() => handleDelete(record.id)}
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
      ),
    },
  ];

  return (
    <>
      {designerMode ? (
        <Layout style={{ minHeight: '100vh', background: '#fff' }}>
          <Sider 
            width={80} 
            style={{ 
              background: '#f6f6f6', 
              borderRight: '1px solid #d9d9d9',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              padding: '16px 0'
            }}
          >
            <Tooltip title="Back to List" placement="right">
              <Button 
                type="text" 
                icon={<ArrowLeftOutlined style={{ fontSize: 20 }} />}
                size="large"
                onClick={handleBackToList}
                style={{ marginBottom: 16, width: 48, height: 48 }}
              />
            </Tooltip>
            <Divider style={{ margin: '8px 0', width: '60%' }} />
            <Tooltip title="Save Process" placement="right">
              <Button 
                type="text" 
                icon={<SaveOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Tooltip title="Undo" placement="right">
              <Button 
                type="text" 
                icon={<UndoOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Tooltip title="Redo" placement="right">
              <Button 
                type="text" 
                icon={<RedoOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Divider style={{ margin: '8px 0', width: '60%' }} />
            <Tooltip title="Zoom In" placement="right">
              <Button 
                type="text" 
                icon={<ZoomInOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Tooltip title="Zoom Out" placement="right">
              <Button 
                type="text" 
                icon={<ZoomOutOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Tooltip title="Fit to Screen" placement="right">
              <Button 
                type="text" 
                icon={<ExpandOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Divider style={{ margin: '8px 0', width: '60%' }} />
            <Tooltip title="Settings" placement="right">
              <Button 
                type="text" 
                icon={<SettingOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
          </Sider>
          <Content style={{ padding: 0, background: '#fff' }}>
            <div style={{ height: '100vh', width: '100%' }}>
              <BpmnEditor 
                initialXml={currentBpmn}
                onSave={(xml, processKey, processName) => handleSave(xml, processKey, processName)}
                processKey={selectedProcess?.key}
                processName={selectedProcess?.name}
              />
            </div>
          </Content>
        </Layout>
      ) : viewerMode ? (
        <Layout style={{ minHeight: '100vh', background: '#fff' }}>
          <Sider 
            width={80} 
            style={{ 
              background: '#f6f6f6', 
              borderRight: '1px solid #d9d9d9',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              padding: '16px 0'
            }}
          >
            <Tooltip title="Back to List" placement="right">
              <Button 
                type="text" 
                icon={<ArrowLeftOutlined style={{ fontSize: 20 }} />}
                size="large"
                onClick={handleBackToList}
                style={{ marginBottom: 16, width: 48, height: 48 }}
              />
            </Tooltip>
            <Divider style={{ margin: '8px 0', width: '60%' }} />
            <Tooltip title="Zoom In" placement="right">
              <Button 
                type="text" 
                icon={<ZoomInOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Tooltip title="Zoom Out" placement="right">
              <Button 
                type="text" 
                icon={<ZoomOutOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
            <Tooltip title="Fit to Screen" placement="right">
              <Button 
                type="text" 
                icon={<ExpandOutlined style={{ fontSize: 20 }} />}
                size="large"
                style={{ marginBottom: 8, width: 48, height: 48 }}
              />
            </Tooltip>
          </Sider>
          <Content style={{ padding: 0, background: '#fff', display: 'flex', flexDirection: 'column' }}>
            {/* BPMN Diagram - Full Width */}
            <div style={{ height: '65vh', width: '100%', marginBottom: '8px', border: '2px solid #d9d9d9', borderRadius: '8px' }}>
              <BpmnEditor 
                initialXml={currentBpmn}
                processKey={selectedProcess?.key}
                processName={selectedProcess?.name}
                readonly={true}
              />
            </div>
            
            {/* Process Info and History - Bottom Section */}
            <div style={{ padding: '0 16px 16px 16px', overflow: 'auto', background: '#fff' }}>
              <Row gutter={[24, 16]}>
                <Col span={8}>
                  {/* Process Instance Info */}
                  {processInstance && (
                    <Card 
                      size="small" 
                      title="Process Instance" 
                      style={{ 
                        height: '280px', 
                        border: '3px solid #1890ff',
                        borderRadius: '8px',
                        boxShadow: '0 4px 8px rgba(0,0,0,0.1)'
                      }}
                      headStyle={{ 
                        background: '#1890ff', 
                        color: 'white', 
                        fontWeight: 'bold' 
                      }}
                    >
                      <Space direction="vertical" style={{ width: '100%' }} size="small">
                        <div>
                          <Typography.Text strong>ID: </Typography.Text>
                          <Typography.Text copyable style={{ fontSize: '12px' }}>{processInstance.id}</Typography.Text>
                        </div>
                        <div>
                          <Typography.Text strong>Status: </Typography.Text>
                          <Tag color={processInstance.suspended ? 'red' : 'green'} style={{ fontWeight: 'bold' }}>
                            {processInstance.suspended ? 'Suspended' : 'Active'}
                          </Tag>
                        </div>
                        <div>
                          <Typography.Text strong>Started: </Typography.Text>
                          <Typography.Text style={{ fontSize: '12px' }}>
                            {new Date(processInstance.startTime).toLocaleString()}
                          </Typography.Text>
                        </div>
                        {processInstance.businessKey && (
                          <div>
                            <Typography.Text strong>Business Key: </Typography.Text>
                            <Typography.Text style={{ fontSize: '12px' }}>{processInstance.businessKey}</Typography.Text>
                          </div>
                        )}
                        <div>
                          <Typography.Text strong>Process Definition: </Typography.Text>
                          <Typography.Text style={{ fontSize: '12px' }}>{processInstance.processDefinitionName}</Typography.Text>
                        </div>
                      </Space>
                    </Card>
                  )}
                </Col>

                <Col span={8}>
                  {/* Task History with Switch Steps */}
                  <Card 
                    size="small" 
                    title="Process Steps" 
                    style={{ 
                      height: '280px', 
                      border: '3px solid #52c41a',
                      borderRadius: '8px',
                      boxShadow: '0 4px 8px rgba(0,0,0,0.1)'
                    }}
                    headStyle={{ 
                      background: '#52c41a', 
                      color: 'white', 
                      fontWeight: 'bold' 
                    }}
                  >
                    <div style={{ height: '200px', overflowY: 'auto' }}>
                      <Steps
                        direction="vertical"
                        size="small"
                        current={taskHistory.filter(task => task.endTime).length}
                        items={[
                          ...taskHistory.map((task, index) => ({
                            title: task.name || `Step ${index + 1}`,
                            status: (task.endTime ? 'finish' : 'process') as 'finish' | 'process' | 'wait' | 'error',
                            description: (
                              <div style={{ fontSize: '11px', color: '#666' }}>
                                <div><strong>Assignee:</strong> {task.assignee || 'Unassigned'}</div>
                                {task.endTime ? (
                                  <div><strong>Completed:</strong> {new Date(task.endTime).toLocaleString()}</div>
                                ) : (
                                  <div><strong>Started:</strong> {task.startTime ? new Date(task.startTime).toLocaleString() : '-'}</div>
                                )}
                                {task.durationInMillis && (
                                  <div><strong>Duration:</strong> {Math.round(task.durationInMillis / 60000)} min</div>
                                )}
                              </div>
                            )
                          })),
                          // Add pending steps if needed
                          ...(taskHistory.length === 0 ? [
                            {
                              title: 'Process Started',
                              status: 'finish' as const,
                              description: <div style={{ fontSize: '11px', color: '#666' }}>Process has been initiated successfully</div>
                            },
                            {
                              title: 'In Progress',
                              status: 'process' as const,
                              description: <div style={{ fontSize: '11px', color: '#666' }}>Current step is being executed</div>
                            },
                            {
                              title: 'Waiting',
                              status: 'wait' as const,
                              description: <div style={{ fontSize: '11px', color: '#666' }}>Next steps are waiting to be processed</div>
                            }
                          ] : [])
                        ]}
                      />
                      {taskHistory.length === 0 && processInstance && (
                        <div style={{ textAlign: 'center', padding: '10px', color: '#999', fontSize: '12px' }}>
                          Process is running - tasks will appear here as they execute
                        </div>
                      )}
                    </div>
                  </Card>
                </Col>

                <Col span={8}>
                  {/* Process Variables */}
                  <Card 
                    size="small" 
                    title="Process Variables" 
                    style={{ 
                      height: '280px', 
                      border: '3px solid #faad14',
                      borderRadius: '8px',
                      boxShadow: '0 4px 8px rgba(0,0,0,0.1)'
                    }}
                    headStyle={{ 
                      background: '#faad14', 
                      color: 'white', 
                      fontWeight: 'bold' 
                    }}
                  >
                    <div style={{ height: '200px', overflowY: 'auto' }}>
                      <Table 
                        dataSource={processVariables}
                        columns={[
                          {
                            title: 'Name',
                            dataIndex: 'name',
                            key: 'name',
                            width: 100,
                            ellipsis: true,
                            render: (text) => <Typography.Text strong style={{ fontSize: '12px' }}>{text}</Typography.Text>
                          },
                          {
                            title: 'Value',
                            dataIndex: 'value',
                            key: 'value',
                            ellipsis: true,
                            render: (value, record) => (
                              <Typography.Text
                                editable={{
                                  onChange: (newValue) => handleUpdateVariable(record.name, newValue),
                                }}
                                style={{ fontSize: '12px' }}
                              >
                                {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                              </Typography.Text>
                            ),
                          },
                        ]}
                        pagination={false}
                        size="small"
                        showHeader={true}
                        bordered={true}
                        style={{
                          border: '2px solid #faad14',
                          borderRadius: '6px'
                        }}
                      />
                      {processVariables.length === 0 && (
                        <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                          <Typography.Text>No variables available</Typography.Text>
                        </div>
                      )}
                    </div>
                  </Card>
                </Col>
              </Row>
            </div>
          </Content>
        </Layout>
      ) : (
        <div style={{ padding: '24px' }}>
          <Row justify="space-between" align="middle" style={{ marginBottom: '24px' }}>
            <Col>
              <Title level={2}>
                <FileTextOutlined style={{ marginRight: '8px' }} />
                Process Designer
              </Title>
            </Col>
            <Col>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreateNew}
                size="large"
              >
                Create New Process
              </Button>
            </Col>
          </Row>

          <Card>
            <Table
              columns={columns}
              dataSource={processes}
              loading={loading}
              rowKey="id"
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
      )}

      {/* BPMN Viewer Modal - keep for backward compatibility */}
      <Modal
        title={`View Process: ${selectedProcess?.name}`}
        open={viewerVisible}
        onCancel={() => setViewerVisible(false)}
        footer={[
          <Button key="close" onClick={() => setViewerVisible(false)}>
            Close
          </Button>
        ]}
        width="90%"
        style={{ top: 20 }}
        destroyOnHidden
      >
        <BpmnEditor
          initialXml={currentBpmn}
          processKey={selectedProcess?.key}
          processName={selectedProcess?.name}
          readonly={true}
        />
      </Modal>
    </>
  );
};

export default ProcessDesigner;
