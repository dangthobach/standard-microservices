import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  message,
  Form,
  Typography,
  Tag,
  Popconfirm,
  Tabs,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  FormOutlined,
  BuildOutlined,
  CopyOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { FormDefinition } from '../types';
import PageBuilder from '../components/pageBuilder/PageBuilder';

const { Title, Text } = Typography;

interface FormField {
  id: string;
  name: string;
  label: string;
  type: 'text' | 'number' | 'email' | 'password' | 'textarea' | 'select' | 'checkbox' | 'radio' | 'date';
  required: boolean;
  placeholder?: string;
  options?: string[];
  validation?: string;
}

interface FormDesigner {
  formKey: string;
  formName: string;
  description: string;
  fields: FormField[];
  processDefinitionKey?: string;
  userTaskId?: string;
}

const FormManagement: React.FC = () => {
  const [legacyForms, setLegacyForms] = useState<FormDefinition[]>([]);
  const [loading, setLoading] = useState(false);
  
  // Form Builder states
  const [formBuilderVisible, setFormBuilderVisible] = useState(false);
  const [editingForm, setEditingForm] = useState<any | null>(null);
  
  // Legacy form states
  const [legacyEditingForm, setLegacyEditingForm] = useState<FormDesigner | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchLegacyForms();
  }, []);

  const fetchLegacyForms = async () => {
    setLoading(true);
    try {
      // Mock data - replace with actual API call
      const mockForms: FormDefinition[] = [
        {
          id: '1',
          key: 'user-registration',
          name: 'User Registration Form',
          version: 1,
          deploymentId: 'dep-1',
          resourceName: 'user-registration.form'
        },
        {
          id: '2',
          key: 'expense-request',
          name: 'Expense Request Form',
          version: 1,
          deploymentId: 'dep-2',
          resourceName: 'expense-request.form'
        }
      ];
      setLegacyForms(mockForms);
    } catch (error) {
      message.error('Failed to fetch legacy forms');
      console.error('Error fetching legacy forms:', error);
    } finally {
      setLoading(false);
    }
  };

  // Form Builder Management Functions
  const handleCreateFormBuilder = () => {
    setEditingForm(null);
    setFormBuilderVisible(true);
  };

  const handleSaveFormBuilder = async (formData: any) => {
    try {
      console.log('Saving form builder form:', formData);
      message.success('Form saved successfully');
      setFormBuilderVisible(false);
      setEditingForm(null);
      fetchLegacyForms();
    } catch (error) {
      message.error('Failed to save form');
      console.error('Error saving form:', error);
    }
  };

  // Legacy Form Management
  const handleSaveForm = async (values: FormDesigner) => {
    try {
      console.log('Form submitted:', values);
      message.success('Form saved successfully!');
      setLegacyEditingForm(null);
      form.resetFields();
      fetchLegacyForms();
    } catch (error) {
      message.error('Failed to save form');
      console.error('Error saving form:', error);
    }
  };

  const handleEditLegacyForm = (record: FormDefinition) => {
    const formDesigner: FormDesigner = {
      formKey: record.key,
      formName: record.name,
      description: `Legacy form: ${record.name}`,
      fields: []
    };
    setLegacyEditingForm(formDesigner);
    // Modal functionality can be added later if needed
  };

  const handleDeleteForm = async (id: string) => {
    try {
      message.success('Form deleted successfully');
      fetchLegacyForms();
    } catch (error) {
      message.error('Failed to delete form');
      console.error('Error deleting form:', error);
    }
  };

  const handleDuplicateForm = async (id: string) => {
    try {
      message.success('Form duplicated successfully');
      fetchLegacyForms();
    } catch (error) {
      message.error('Failed to duplicate form');
      console.error('Error duplicating form:', error);
    }
  };

  // Column definitions for legacy forms table
  const legacyFormColumns = [
    {
      title: 'Form Key',
      dataIndex: 'key',
      key: 'key',
      render: (text: string) => <Text strong>{text}</Text>
    },
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: 'Version',
      dataIndex: 'version',
      key: 'version',
      render: (version: number) => <Tag color="blue">v{version}</Tag>
    },
    {
      title: 'Resource',
      dataIndex: 'resourceName',
      key: 'resourceName',
      render: (text: string) => <Text code>{text}</Text>
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: FormDefinition) => (
        <Space>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => message.info('Preview functionality coming soon')}
          />
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditLegacyForm(record)}
          />
          <Button
            size="small"
            icon={<CopyOutlined />}
            onClick={() => handleDuplicateForm(record.id)}
          />
          <Popconfirm
            title="Delete this form?"
            onConfirm={() => handleDeleteForm(record.id)}
            okText="Delete"
            cancelText="Cancel"
          >
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
            />
          </Popconfirm>
        </Space>
      )
    }
  ];

  const tabItems = [
    {
      key: 'page-builder',
      label: (
        <span>
          <BuildOutlined />
          Page Builder Forms
        </span>
      ),
      children: (
        <Card>
          <div style={{ marginBottom: 16 }}>
            <Space>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreateFormBuilder}
              >
                Create New Form
              </Button>
              <Button icon={<ReloadOutlined />} onClick={fetchLegacyForms}>
                Refresh
              </Button>
            </Space>
          </div>
          
          <Table
            columns={legacyFormColumns}
            dataSource={legacyForms}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        </Card>
      )
    },
    {
      key: 'legacy',
      label: (
        <span>
          <FormOutlined />
          Legacy Forms
        </span>
      ),
      children: (
        <Card>
          <div style={{ marginBottom: 16 }}>
            <Space>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  setLegacyEditingForm(null);
                  // Modal functionality can be added later if needed
                }}
              >
                Create Legacy Form
              </Button>
              <Button icon={<ReloadOutlined />} onClick={fetchLegacyForms}>
                Refresh
              </Button>
            </Space>
          </div>
          
          <Table
            columns={legacyFormColumns}
            dataSource={legacyForms}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        </Card>
      )
    }
  ];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>Form Management</Title>
        <Text type="secondary">
          Manage your forms using the modern Page Builder or create legacy forms
        </Text>
      </div>

      <Tabs
        items={tabItems}
        defaultActiveKey="page-builder"
        size="large"
      />

      {/* Legacy Form Creation Modal */}
      <Form
        form={form}
        name="form-management"
        onFinish={handleSaveForm}
        layout="vertical"
        initialValues={legacyEditingForm || {}}
      >
        {/* Modal implementation would go here */}
      </Form>

      {/* Page Builder Full Screen */}
      {formBuilderVisible && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          zIndex: 1000,
          backgroundColor: 'white'
        }}>
          <div style={{
            height: '100vh',
            display: 'flex',
            flexDirection: 'column'
          }}>
            {/* Header with title and close button */}
            <div style={{
              padding: '16px 24px',
              borderBottom: '1px solid #f0f0f0',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              backgroundColor: 'white',
              zIndex: 1001
            }}>
              <Title level={4} style={{ margin: 0 }}>
                {editingForm?.formName ? `Edit: ${editingForm.formName}` : 'Create New Form'}
              </Title>
              <Button 
                type="text" 
                size="large"
                onClick={() => {
                  setFormBuilderVisible(false);
                  setEditingForm(null);
                }}
                style={{ fontSize: '16px' }}
              >
                ✕
              </Button>
            </div>
            
            {/* PageBuilder Container */}
            <div style={{ flex: 1, overflow: 'hidden' }}>
              <PageBuilder
                onSave={(schema) => handleSaveFormBuilder(schema)}
                onLoad={() => editingForm}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FormManagement;
