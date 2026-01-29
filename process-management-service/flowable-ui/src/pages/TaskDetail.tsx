import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Card, 
  Form, 
  Input, 
  Button, 
  Descriptions, 
  Space, 
  message, 
  Spin, 
  Row,
  Col,
  Tag,
  Typography,
  InputNumber,
  Checkbox
} from 'antd';
import { 
  ArrowLeftOutlined, 
  SaveOutlined, 
  CheckCircleOutlined,
  UserOutlined,
  ClockCircleOutlined,
  FileTextOutlined
} from '@ant-design/icons';
import { taskApi, processApi } from '../services/flowableApi';
import { Task, Variable } from '../types';
import ProcessDiagram from '../components/ProcessDiagram';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface TaskFormData {
  [key: string]: any;
}

const TaskDetail: React.FC = () => {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();
  const [task, setTask] = useState<Task | null>(null);
  const [variables, setVariables] = useState<Variable[]>([]);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [processBpmn, setProcessBpmn] = useState<string>('');
  const [currentActivity, setCurrentActivity] = useState<string>('');

  const fetchTaskData = useCallback(async () => {
    if (!taskId) return;
    
    setLoading(true);
    try {
      const [taskData, variablesData] = await Promise.all([
        taskApi.getTask(taskId),
        taskApi.getTaskVariables(taskId)
      ]);
      
      setTask(taskData);
      
      // Convert variables to array format
      const variablesArray = Object.entries(variablesData).map(([name, value]) => ({
        name,
        value,
        type: typeof value
      }));
      setVariables(variablesArray);
      
      // Set form values
      const formData: TaskFormData = {};
      variablesArray.forEach(variable => {
        formData[variable.name] = variable.value;
      });
      form.setFieldsValue(formData);
      
      // Load BPMN diagram if process instance exists
      if (taskData.processInstanceId) {
        try {
          const bpmnXml = await processApi.getProcessBpmn('helloProcess'); // You might need to get the actual process key
          setProcessBpmn(bpmnXml);
          setCurrentActivity(taskData.taskDefinitionKey || '');
        } catch (error) {
          console.error('Failed to load BPMN diagram:', error);
        }
      }
      
    } catch (error) {
      message.error('Failed to fetch task data');
      console.error('Error fetching task data:', error);
    } finally {
      setLoading(false);
    }
  }, [taskId, form]);

  useEffect(() => {
    if (taskId) {
      fetchTaskData();
    }
  }, [taskId, fetchTaskData]);

  const handleSave = async (values: TaskFormData) => {
    if (!taskId) return;
    
    try {
      await taskApi.setTaskVariables(taskId, values);
      message.success('Task variables saved successfully');
      fetchTaskData(); // Refresh data
    } catch (error) {
      message.error('Failed to save task variables');
      console.error('Error saving task variables:', error);
    }
  };

  const handleComplete = async (values: TaskFormData) => {
    if (!taskId) return;
    
    try {
      await taskApi.completeTask(taskId, values);
      message.success('Task completed successfully');
      navigate('/tasks'); // Navigate back to task list
    } catch (error) {
      message.error('Failed to complete task');
      console.error('Error completing task:', error);
    }
  };

  const renderFormField = (variable: Variable) => {
    const { name, type } = variable;
    
    switch (type) {
      case 'string':
        if (name.toLowerCase().includes('description') || name.toLowerCase().includes('comment')) {
          return <TextArea rows={4} placeholder={`Enter ${name}`} />;
        }
        return <Input placeholder={`Enter ${name}`} />;
        
      case 'number':
        return <InputNumber style={{ width: '100%' }} placeholder={`Enter ${name}`} />;
        
      case 'boolean':
        return <Checkbox>Yes</Checkbox>;
        
      case 'object':
        return <TextArea rows={3} placeholder={`Enter ${name} (JSON format)`} />;
        
      default:
        return <Input placeholder={`Enter ${name}`} />;
    }
  };

  const getTaskStatusColor = (task: Task) => {
    if (task.assignee) return 'green';
    return 'orange';
  };

  const getTaskStatusText = (task: Task) => {
    if (task.assignee) return 'Assigned';
    return 'Unassigned';
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
        <p>Loading task details...</p>
      </div>
    );
  }

  if (!task) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <p>Task not found</p>
        <Button onClick={() => navigate('/tasks')}>Back to Tasks</Button>
      </div>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      {/* Header */}
      <div style={{ marginBottom: '24px' }}>
        <Button 
          icon={<ArrowLeftOutlined />} 
          onClick={() => navigate('/tasks')}
          style={{ marginBottom: '16px' }}
        >
          Back to Tasks
        </Button>
        
        <Title level={2}>
          <FileTextOutlined style={{ marginRight: '8px' }} />
          {task.name}
        </Title>
      </div>

      <Row gutter={24}>
        {/* Task Information */}
        <Col span={12}>
          <Card title="Task Information" style={{ marginBottom: '24px' }}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Task ID">
                <Text code>{task.id}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Name">
                {task.name}
              </Descriptions.Item>
              <Descriptions.Item label="Description">
                {task.description || 'No description'}
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={getTaskStatusColor(task)}>
                  {getTaskStatusText(task)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Assignee">
                {task.assignee ? (
                  <Tag icon={<UserOutlined />} color="blue">
                    {task.assignee}
                  </Tag>
                ) : (
                  <Tag color="orange">Unassigned</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Created">
                <Tag icon={<ClockCircleOutlined />}>
                  {new Date(task.created).toLocaleString()}
                </Tag>
              </Descriptions.Item>
              {task.dueDate && (
                <Descriptions.Item label="Due Date">
                  {new Date(task.dueDate).toLocaleString()}
                </Descriptions.Item>
              )}
              {task.processInstanceId && (
                <Descriptions.Item label="Process Instance">
                  <Text code>{task.processInstanceId}</Text>
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>

          {/* Task Form */}
          <Card title="Task Form" extra={
            <Space>
              <Button 
                icon={<SaveOutlined />} 
                onClick={() => form.submit()}
              >
                Save
              </Button>
              <Button 
                type="primary" 
                icon={<CheckCircleOutlined />} 
                onClick={() => form.submit()}
              >
                Complete Task
              </Button>
            </Space>
          }>
            <Form
              form={form}
              layout="vertical"
              onFinish={handleComplete}
              onValuesChange={(changedValues, allValues) => {
                // Auto-save on form change
                handleSave(allValues);
              }}
            >
              {variables.map(variable => (
                <Form.Item
                  key={variable.name}
                  label={variable.name}
                  name={variable.name}
                  rules={[
                    {
                      required: variable.name.toLowerCase().includes('required'),
                      message: `${variable.name} is required`
                    }
                  ]}
                >
                  {renderFormField(variable)}
                </Form.Item>
              ))}
              
              {variables.length === 0 && (
                <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
                  No form fields defined for this task
                </div>
              )}
            </Form>
          </Card>
        </Col>

        {/* Process Diagram */}
        <Col span={12}>
          {processBpmn && (
            <ProcessDiagram
              bpmnXml={processBpmn}
              currentActivity={currentActivity}
              highlightedActivities={[currentActivity]}
              completedActivities={[]}
            />
          )}
        </Col>
      </Row>
    </div>
  );
};

export default TaskDetail;
