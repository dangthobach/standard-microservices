import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, message, Space, Tag } from 'antd';
import { CheckCircleOutlined, UserOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { taskApi } from '../services/flowableApi';
import { Task } from '../types';

const TaskInbox: React.FC = () => {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [claimModalVisible, setClaimModalVisible] = useState(false);
  const [completeModalVisible, setCompleteModalVisible] = useState(false);
  const [claimForm] = Form.useForm();
  const [completeForm] = Form.useForm();

  const fetchTasks = async () => {
    setLoading(true);
    try {
      const data = await taskApi.getTasks();
      setTasks(data);
    } catch (error) {
      message.error('Failed to fetch tasks');
      console.error('Error fetching tasks:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, []);

  const handleClaim = async (values: { userId: string }) => {
    if (!selectedTask) return;
    
    try {
      await taskApi.claimTask(selectedTask.id, values.userId);
      message.success('Task claimed successfully');
      setClaimModalVisible(false);
      claimForm.resetFields();
      fetchTasks();
    } catch (error) {
      message.error('Failed to claim task');
      console.error('Error claiming task:', error);
    }
  };

  const handleComplete = async (values: { variables: string }) => {
    if (!selectedTask) return;
    
    try {
      let variables = {};
      if (values.variables) {
        try {
          variables = JSON.parse(values.variables);
        } catch (e) {
          message.error('Invalid JSON format for variables');
          return;
        }
      }
      
      await taskApi.completeTask(selectedTask.id, variables);
      message.success('Task completed successfully');
      setCompleteModalVisible(false);
      completeForm.resetFields();
      fetchTasks();
    } catch (error) {
      message.error('Failed to complete task');
      console.error('Error completing task:', error);
    }
  };

  const columns = [
    {
      title: 'Task Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Assignee',
      dataIndex: 'assignee',
      key: 'assignee',
      render: (assignee: string) => assignee || <Tag color="orange">Unassigned</Tag>,
    },
    {
      title: 'Created',
      dataIndex: 'created',
      key: 'created',
      render: (created: string) => new Date(created).toLocaleString(),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Task) => (
        <Space>
          <Button
            type="primary"
            icon={<UserOutlined />}
            onClick={() => {
              setSelectedTask(record);
              setClaimModalVisible(true);
            }}
            disabled={!!record.assignee}
          >
            Claim
          </Button>
          <Button
            type="default"
            icon={<CheckCircleOutlined />}
            onClick={() => {
              setSelectedTask(record);
              setCompleteModalVisible(true);
            }}
            disabled={!record.assignee}
          >
            Complete
          </Button>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => {
              navigate(`/tasks/${record.id}`);
            }}
          >
            View
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <h2>Task Inbox</h2>
      
      <Table
        columns={columns}
        dataSource={tasks}
        loading={loading}
        rowKey="id"
        pagination={{ pageSize: 10 }}
      />

      {/* Claim Modal */}
      <Modal
        title="Claim Task"
        open={claimModalVisible}
        onCancel={() => setClaimModalVisible(false)}
        footer={null}
      >
        <Form form={claimForm} onFinish={handleClaim}>
          <Form.Item
            name="userId"
            label="User ID"
            rules={[{ required: true, message: 'Please enter user ID' }]}
          >
            <Input placeholder="Enter user ID" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Claim
              </Button>
              <Button onClick={() => setClaimModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Complete Modal */}
      <Modal
        title="Complete Task"
        open={completeModalVisible}
        onCancel={() => setCompleteModalVisible(false)}
        footer={null}
      >
        <Form form={completeForm} onFinish={handleComplete}>
          <Form.Item
            name="variables"
            label="Variables (JSON)"
            help="Enter variables in JSON format, e.g., {'key': 'value'}"
          >
            <Input.TextArea
              placeholder='{"key": "value"}'
              rows={4}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Complete
              </Button>
              <Button onClick={() => setCompleteModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TaskInbox;
