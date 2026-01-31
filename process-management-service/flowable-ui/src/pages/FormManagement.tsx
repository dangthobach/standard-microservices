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
  Modal,
  Input,
  Upload
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  FormOutlined,
  BuildOutlined,
  ReloadOutlined,
  UploadOutlined
} from '@ant-design/icons';
import { FormDefinition } from '../types';
import PageBuilder from '../components/pageBuilder/PageBuilder';
import { formApi } from '../services/flowableApi';

const { Title, Text } = Typography;

interface FormDesigner {
  formKey: string;
  formName: string;
  description: string;
}

const FormManagement: React.FC = () => {
  const [legacyForms, setLegacyForms] = useState<FormDefinition[]>([]);
  const [loading, setLoading] = useState(false);

  // Form Builder states
  const [formBuilderVisible, setFormBuilderVisible] = useState(false);
  const [editingForm, setEditingForm] = useState<any | null>(null);

  // Legacy form states
  const [legacyModalVisible, setLegacyModalVisible] = useState(false);
  const [fileList, setFileList] = useState<any[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchLegacyForms();
  }, []);

  const fetchLegacyForms = async () => {
    setLoading(true);
    try {
      const data = await formApi.getForms();
      setLegacyForms(data);
    } catch (error) {
      message.error('Failed to fetch forms');
      console.error('Error fetching forms:', error);
    } finally {
      setLoading(false);
    }
  };

  // Form Builder Management Functions (Placeholder for now, focused on Legacy/Repo forms)
  const handleCreateFormBuilder = () => {
    setEditingForm(null);
    setFormBuilderVisible(true);
  };

  const handleSaveFormBuilder = async (formData: any) => {
    // Logic for saving JSON-based form builder forms would go here.
    // For now, we reuse the deployment API if we can save it as a JSON file.
    message.info("Save logic for builder needs backend support for raw JSON save");
    setFormBuilderVisible(false);
  };

  // Legacy Form Management
  const handleSaveLegacyForm = async (values: any) => {
    if (fileList.length === 0) {
      message.error("Please upload a Form file (.form or .json)");
      return;
    }
    const file = fileList[0].originFileObj;

    try {
      await formApi.deployForm(file, values.formKey, values.formName);
      message.success('Form deployed successfully!');
      setLegacyModalVisible(false);
      form.resetFields();
      setFileList([]);
      fetchLegacyForms();
    } catch (error) {
      message.error('Failed to deploy form');
      console.error('Error saving form:', error);
    }
  };

  const handleEditLegacyForm = (record: FormDefinition) => {
    message.info("Edit functionality requires Model Editor");
  };

  const handleDeleteForm = async (deploymentId: string) => {
    try {
      await formApi.deleteForm(deploymentId);
      message.success('Form deleted successfully');
      fetchLegacyForms();
    } catch (error) {
      message.error('Failed to delete form');
      console.error('Error deleting form:', error);
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
      title: 'Deployment ID',
      dataIndex: 'deploymentId',
      key: 'deploymentId',
      render: (text: string) => <Text type="secondary" style={{ fontSize: '12px' }}>{text}</Text>
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: FormDefinition) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditLegacyForm(record)}
            disabled
          />
          <Popconfirm
            title="Delete this form?"
            onConfirm={() => handleDeleteForm(record.deploymentId)}
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

  const uploadProps = {
    onRemove: (file: any) => {
      setFileList([]);
    },
    beforeUpload: (file: any) => {
      const isForm = file.name.endsWith('.form') || file.name.endsWith('.json');
      if (!isForm) {
        message.error('You can only upload .form or .json files!');
        return Upload.LIST_IGNORE;
      }
      setFileList([file]);
      return false;
    },
    fileList,
  };

  const tabItems = [
    {
      key: 'repo-forms',
      label: (
        <span>
          <FormOutlined />
          Form Definitions
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
                  setLegacyModalVisible(true);
                  form.resetFields();
                  setFileList([]);
                }}
              >
                Deploy New Form
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
      key: 'page-builder',
      label: (
        <span>
          <BuildOutlined />
          Page Builder (Beta)
        </span>
      ),
      children: (
        <Card>
          <div className="text-center py-8">
            <BuildOutlined style={{ fontSize: '48px', color: '#ccc', marginBottom: '16px' }} />
            <Title level={4}>Page Builder Integration Pending</Title>
            <Text type="secondary">The visual page builder is currently in development.</Text>
          </div>
        </Card>
      )
    }
  ];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>Form Management</Title>
        <Text type="secondary">
          Manage your form definitions and deployments.
        </Text>
      </div>

      <Tabs
        items={tabItems}
        defaultActiveKey="repo-forms"
        size="large"
      />

      {/* Legacy Form Creation Modal */}
      <Modal
        title="Deploy New Form"
        open={legacyModalVisible}
        onCancel={() => setLegacyModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setLegacyModalVisible(false)}>
            Cancel
          </Button>,
          <Button key="save" type="primary" onClick={() => form.submit()}>
            Deploy
          </Button>
        ]}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveLegacyForm}
        >
          <Form.Item
            label="Form Key (Optional)"
            name="formKey"
          >
            <Input placeholder="Enter unique form key" />
          </Form.Item>
          <Form.Item
            label="Form Name (Optional)"
            name="formName"
          >
            <Input placeholder="Enter form name" />
          </Form.Item>
          <Form.Item
            label="Form File"
            required
            tooltip="Upload a .form or .json file"
          >
            <Upload {...uploadProps} maxCount={1}>
              <Button icon={<UploadOutlined />}>Select File</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

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
