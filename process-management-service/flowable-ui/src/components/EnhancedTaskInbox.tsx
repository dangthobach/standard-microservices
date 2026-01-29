import React, { useState, useEffect, useCallback } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Select,
  Input,
  DatePicker,
  Card,
  Row,
  Col,
  Tooltip,
  notification,
  Modal,
  Form,
  Typography,
  Badge,
  Divider,
  Checkbox,
  message,
  Popconfirm,
  Progress
} from 'antd';
import type { ColumnsType, TableRowSelection } from 'antd/es/table/interface';
import {
  ReloadOutlined,
  FilterOutlined,
  CheckOutlined,
  UserOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  SettingOutlined,
  UnorderedListOutlined,
  ClearOutlined
} from '@ant-design/icons';
import { taskApi } from '../services/flowableApi';
import { Task, TaskFilter, BatchResult, PaginatedResponse } from '../types';
import webSocketService, { TaskNotification } from '../services/webSocketService';
import dayjs from '../utils/dayjs';

const { Search } = Input;
const { RangePicker } = DatePicker;
const { Option } = Select;
const { Title, Text } = Typography;
const { confirm } = Modal;

interface EnhancedTaskInboxProps {
  showProcessInstanceFilter?: boolean;
  defaultProcessInstanceId?: string;
}

const EnhancedTaskInbox: React.FC<EnhancedTaskInboxProps> = ({
  showProcessInstanceFilter = true,
  defaultProcessInstanceId
}) => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number, range: [number, number]) => 
      `${range[0]}-${range[1]} of ${total} tasks`,
  });

  const [filters, setFilters] = useState<TaskFilter>({
    page: 0,
    size: 20,
    sortBy: 'created',
    sortOrder: 'desc',
    processInstanceId: defaultProcessInstanceId
  });

  const [filterVisible, setFilterVisible] = useState(false);
  const [batchModalVisible, setBatchModalVisible] = useState(false);
  const [batchOperation, setBatchOperation] = useState<'claim' | 'complete' | 'assign'>('claim');
  const [batchForm] = Form.useForm();

  // Real-time updates
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const [newTasksCount, setNewTasksCount] = useState(0);

  const fetchTasks = useCallback(async (newFilters?: TaskFilter) => {
    setLoading(true);
    try {
      const filterParams = newFilters || filters;
      const response: PaginatedResponse<Task> = await taskApi.getTasksPaginated(filterParams);
      
      setTasks(response.content);
      setPagination(prev => ({
        ...prev,
        current: response.page + 1,
        total: response.totalElements,
        pageSize: response.size
      }));
      
      setLastUpdated(new Date());
      setNewTasksCount(0); // Reset new tasks count when refreshing
      
    } catch (error) {
      console.error('Error fetching tasks:', error);
      notification.error({
        message: 'Error',
        description: 'Failed to fetch tasks'
      });
    } finally {
      setLoading(false);
    }
  }, [filters]);

  // WebSocket integration for real-time updates
  useEffect(() => {
    const unsubscribe = webSocketService.onTaskNotification((taskNotification: TaskNotification) => {
      if (taskNotification.type === 'TASK_ASSIGNED' || 
          taskNotification.type === 'TASK_CREATED' || 
          taskNotification.type === 'TASK_COMPLETED') {
        setNewTasksCount(prev => prev + 1);
        
        // Show notification
        notification.info({
          message: 'Task Update',
          description: `Task "${taskNotification.taskName}" has been ${taskNotification.type.toLowerCase().replace('task_', '')}`,
          duration: 4,
          placement: 'topRight'
        });
      }
    });

    return unsubscribe;
  }, []);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const handleTableChange = (paginationParams: any, tableFilters: any, sorter: any) => {
    const newFilters: TaskFilter = {
      ...filters,
      page: paginationParams.current - 1,
      size: paginationParams.pageSize,
    };

    if (sorter.field) {
      newFilters.sortBy = sorter.field;
      newFilters.sortOrder = sorter.order === 'ascend' ? 'asc' : 'desc';
    }

    setFilters(newFilters);
    fetchTasks(newFilters);
  };

  const handleFilter = (values: any) => {
    const newFilters: TaskFilter = {
      ...filters,
      page: 0, // Reset to first page when filtering
      assignee: values.assignee,
      processDefinitionKey: values.processDefinitionKey,
      taskDefinitionKey: values.taskDefinitionKey,
      createdAfter: values.createdRange?.[0]?.format('YYYY-MM-DD'),
      createdBefore: values.createdRange?.[1]?.format('YYYY-MM-DD'),
      dueAfter: values.dueRange?.[0]?.format('YYYY-MM-DD'),
      dueBefore: values.dueRange?.[1]?.format('YYYY-MM-DD'),
    };

    setFilters(newFilters);
    fetchTasks(newFilters);
    setFilterVisible(false);
  };

  const handleClearFilters = () => {
    const newFilters: TaskFilter = {
      page: 0,
      size: filters.size,
      sortBy: 'created',
      sortOrder: 'desc',
      processInstanceId: defaultProcessInstanceId
    };
    
    setFilters(newFilters);
    fetchTasks(newFilters);
    setFilterVisible(false);
  };

  const handleSearch = (value: string) => {
    // For simplicity, searching in task name - you can extend this
    const newFilters: TaskFilter = {
      ...filters,
      page: 0,
      // Add search parameter to your API if needed
    };
    
    setFilters(newFilters);
    fetchTasks(newFilters);
  };

  // Single task operations
  const handleClaimTask = async (taskId: string) => {
    try {
      await taskApi.claimTask(taskId, 'current-user'); // Replace with actual user ID
      message.success('Task claimed successfully');
      fetchTasks();
    } catch (error) {
      message.error('Failed to claim task');
    }
  };

  const handleCompleteTask = async (taskId: string) => {
    confirm({
      title: 'Complete Task',
      content: 'Are you sure you want to complete this task?',
      onOk: async () => {
        try {
          await taskApi.completeTask(taskId);
          message.success('Task completed successfully');
          fetchTasks();
        } catch (error) {
          message.error('Failed to complete task');
        }
      }
    });
  };

  // Batch operations
  const handleBatchOperation = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('Please select tasks to perform batch operation');
      return;
    }
    setBatchModalVisible(true);
  };

  const handleBatchSubmit = async (values: any) => {
    const taskIds = selectedRowKeys as string[];
    let result: BatchResult;

    try {
      switch (batchOperation) {
        case 'claim':
          result = await taskApi.claimTasksBatch(taskIds, values.userId || 'current-user');
          break;
        case 'complete':
          result = await taskApi.completeTasksBatch(taskIds, values.variables);
          break;
        case 'assign':
          const assignments = taskIds.map(taskId => ({
            taskId,
            assignee: values.assignee
          }));
          result = await taskApi.assignTasksBatch(assignments);
          break;
        default:
          throw new Error('Invalid batch operation');
      }

      notification.success({
        message: 'Batch Operation Completed',
        description: `Successfully processed ${result.successCount} tasks, ${result.failureCount} failed`,
        duration: 5
      });

      setBatchModalVisible(false);
      batchForm.resetFields();
      setSelectedRowKeys([]);
      fetchTasks();

    } catch (error) {
      notification.error({
        message: 'Batch Operation Failed',
        description: 'Failed to perform batch operation'
      });
    }
  };

  // Row selection
  const rowSelection: TableRowSelection<Task> = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(newSelectedRowKeys);
    },
    getCheckboxProps: (record: Task) => ({
      disabled: !record.id, // Disable selection for completed tasks
    }),
  };

  // Table columns
  const columns: ColumnsType<Task> = [
    {
      title: 'Task Name',
      dataIndex: 'name',
      key: 'name',
      sorter: true,
      render: (text: string, record: Task) => (
        <Space direction="vertical" size="small">
          <Text strong>{text}</Text>
          {record.description && (
            <Text type="secondary" style={{ fontSize: '12px' }}>
              {record.description}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Assignee',
      dataIndex: 'assignee',
      key: 'assignee',
      render: (assignee: string) => (
        assignee ? (
          <Tag icon={<UserOutlined />} color="blue">
            {assignee}
          </Tag>
        ) : (
          <Tag color="default">Unassigned</Tag>
        )
      ),
    },
    {
      title: 'Process',
      dataIndex: 'processInstanceId',
      key: 'processInstanceId',
      render: (processInstanceId: string) => (
        <Text code style={{ fontSize: '12px' }}>
          {processInstanceId?.substring(0, 8)}...
        </Text>
      ),
    },
    {
      title: 'Created',
      dataIndex: 'created',
      key: 'created',
      sorter: true,
      render: (created: string) => (
        <Tooltip title={dayjs(created).format('YYYY-MM-DD HH:mm:ss')}>
          {dayjs(created).fromNow()}
        </Tooltip>
      ),
    },
    {
      title: 'Due Date',
      dataIndex: 'dueDate',
      key: 'dueDate',
      sorter: true,
      render: (dueDate: string) => {
        if (!dueDate) return <Text type="secondary">-</Text>;
        
        const due = dayjs(dueDate);
        const isOverdue = due.isBefore(dayjs());
        const isNearDue = due.diff(dayjs(), 'hour') <= 24;
        
        return (
          <Tooltip title={due.format('YYYY-MM-DD HH:mm:ss')}>
            <Tag 
              color={isOverdue ? 'red' : isNearDue ? 'orange' : 'green'}
              icon={<ClockCircleOutlined />}
            >
              {due.fromNow()}
            </Tag>
          </Tooltip>
        );
      },
    },
    {
      title: 'Priority',
      key: 'priority',
      render: () => (
        <Tag color="default">Normal</Tag> // You can extend this based on your task priority logic
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Task) => (
        <Space size="small">
          {!record.assignee && (
            <Button
              type="link"
              size="small"
              icon={<UserOutlined />}
              onClick={() => handleClaimTask(record.id)}
            >
              Claim
            </Button>
          )}
          {record.assignee && (
            <Button
              type="link"
              size="small"
              icon={<CheckOutlined />}
              onClick={() => handleCompleteTask(record.id)}
            >
              Complete
            </Button>
          )}
          <Button
            type="link"
            size="small"
            icon={<SettingOutlined />}
            href={`/tasks/${record.id}`}
          >
            Details
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <div style={{ marginBottom: '16px' }}>
          <Row justify="space-between" align="middle">
            <Col>
              <Space align="center">
                <Title level={3} style={{ margin: 0 }}>
                  Task Inbox
                </Title>
                {newTasksCount > 0 && (
                  <Badge count={newTasksCount} size="small">
                    <Button
                      type="primary"
                      icon={<ReloadOutlined />}
                      onClick={() => fetchTasks()}
                      size="small"
                    >
                      Refresh
                    </Button>
                  </Badge>
                )}
                <Text type="secondary">
                  Last updated: {dayjs(lastUpdated).format('HH:mm:ss')}
                </Text>
              </Space>
            </Col>
            <Col>
              <Space>
                <Search
                  placeholder="Search tasks..."
                  allowClear
                  style={{ width: 250 }}
                  onSearch={handleSearch}
                />
                <Button
                  icon={<FilterOutlined />}
                  onClick={() => setFilterVisible(true)}
                >
                  Filters
                </Button>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={() => fetchTasks()}
                  loading={loading}
                />
              </Space>
            </Col>
          </Row>
        </div>

        {/* Batch Operations Bar */}
        {selectedRowKeys.length > 0 && (
          <div style={{ marginBottom: '16px', padding: '12px', backgroundColor: '#e6f7ff', borderRadius: '6px' }}>
            <Row justify="space-between" align="middle">
              <Col>
                <Space>
                  <Text strong>
                    {selectedRowKeys.length} task(s) selected
                  </Text>
                  <Divider type="vertical" />
                  <Button
                    type="link"
                    size="small"
                    onClick={() => setSelectedRowKeys([])}
                    icon={<ClearOutlined />}
                  >
                    Clear Selection
                  </Button>
                </Space>
              </Col>
              <Col>
                <Space>
                  <Select
                    value={batchOperation}
                    onChange={setBatchOperation}
                    style={{ width: 120 }}
                    size="small"
                  >
                    <Option value="claim">Claim</Option>
                    <Option value="assign">Assign</Option>
                    <Option value="complete">Complete</Option>
                  </Select>
                  <Button
                    type="primary"
                    size="small"
                    icon={<UnorderedListOutlined />}
                    onClick={handleBatchOperation}
                  >
                    Execute
                  </Button>
                </Space>
              </Col>
            </Row>
          </div>
        )}

        <Table
          columns={columns}
          dataSource={tasks}
          rowKey="id"
          loading={loading}
          pagination={pagination}
          onChange={handleTableChange}
          rowSelection={rowSelection}
          scroll={{ x: 1000 }}
          size="small"
        />
      </Card>

      {/* Filter Modal */}
      <Modal
        title="Filter Tasks"
        open={filterVisible}
        onCancel={() => setFilterVisible(false)}
        footer={null}
        width={600}
      >
        <Form
          layout="vertical"
          onFinish={handleFilter}
          initialValues={{
            assignee: filters.assignee,
            processDefinitionKey: filters.processDefinitionKey,
            taskDefinitionKey: filters.taskDefinitionKey,
          }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="assignee" label="Assignee">
                <Input placeholder="Enter assignee..." />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="processDefinitionKey" label="Process Type">
                <Select placeholder="Select process type..." allowClear>
                  <Option value="orderProcess">Order Process</Option>
                  <Option value="approvalProcess">Approval Process</Option>
                  <Option value="helloProcess">Hello Process</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="createdRange" label="Created Date Range">
                <RangePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="dueRange" label="Due Date Range">
                <RangePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="taskDefinitionKey" label="Task Type">
            <Select placeholder="Select task type..." allowClear>
              <Option value="userTask">User Task</Option>
              <Option value="reviewTask">Review Task</Option>
              <Option value="approvalTask">Approval Task</Option>
            </Select>
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Apply Filters
              </Button>
              <Button onClick={handleClearFilters}>
                Clear All
              </Button>
              <Button onClick={() => setFilterVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Batch Operation Modal */}
      <Modal
        title={`Batch ${batchOperation.charAt(0).toUpperCase() + batchOperation.slice(1)} Tasks`}
        open={batchModalVisible}
        onCancel={() => setBatchModalVisible(false)}
        footer={null}
        width={500}
      >
        <div style={{ marginBottom: '16px' }}>
          <Text>
            This operation will be applied to <strong>{selectedRowKeys.length}</strong> selected task(s).
          </Text>
        </div>

        <Form
          form={batchForm}
          layout="vertical"
          onFinish={handleBatchSubmit}
        >
          {batchOperation === 'claim' && (
            <Form.Item
              name="userId"
              label="User ID"
              rules={[{ required: true, message: 'Please enter user ID' }]}
            >
              <Input placeholder="Enter user ID to claim tasks..." />
            </Form.Item>
          )}

          {batchOperation === 'assign' && (
            <Form.Item
              name="assignee"
              label="Assignee"
              rules={[{ required: true, message: 'Please enter assignee' }]}
            >
              <Input placeholder="Enter assignee..." />
            </Form.Item>
          )}

          {batchOperation === 'complete' && (
            <Form.Item name="variables" label="Variables (Optional)">
              <Input.TextArea
                rows={4}
                placeholder="Enter variables in JSON format (optional)..."
              />
            </Form.Item>
          )}

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Execute Batch Operation
              </Button>
              <Button onClick={() => setBatchModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default EnhancedTaskInbox;
