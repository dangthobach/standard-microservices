import React, { useCallback, useMemo, useState } from 'react';
import { 
  Layout,
  Card, 
  Typography, 
  Form, 
  Input, 
  InputNumber, 
  Select, 
  Switch, 
  Button, 
  Space, 
  Empty,
  Tabs,
  Row,
  Col,
  Badge,
  Tag,
  message,
  Modal
} from 'antd';
import { 
  DeleteOutlined, 
  PlusOutlined, 
  ArrowLeftOutlined,
  SaveOutlined,
  CopyOutlined,
  ReloadOutlined,
  InfoCircleOutlined,
  SettingOutlined
} from '@ant-design/icons';
import { usePageBuilderStore } from '../../store/pageBuilderStore';
import { WidgetType, GridSection, StackSection } from '../../types/pageBuilder';
import WidgetRenderer from './WidgetRenderer';

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;
const { TabPane } = Tabs;
const { Content, Header } = Layout;

interface PropertyEditorProps {
  onClose: () => void;
}

const PropertyEditor: React.FC<PropertyEditorProps> = ({ onClose }) => {
  const {
    schema,
    selectedItemId,
    selectedSectionId,
    updateItem,
    removeItem,
    duplicateItem
  } = usePageBuilderStore();

  const [form] = Form.useForm();
  const [activeTab, setActiveTab] = useState('properties');

  const selectedSection = useMemo(() => {
    return schema.sections.find(section => section.id === selectedSectionId);
  }, [schema.sections, selectedSectionId]);

  const selectedItem = useMemo(() => {
    if (!selectedSection || !selectedItemId) return null;
    
    if (selectedSection.type === 'grid') {
      return (selectedSection as GridSection).items.find(item => item.id === selectedItemId);
    } else if (selectedSection.type === 'stack') {
      return (selectedSection as StackSection).children.find(item => item.id === selectedItemId);
    }
    
    return null;
  }, [selectedSection, selectedItemId]);

  const handleWidgetPropsChange = useCallback((changedValues: any, allValues: any) => {
    if (selectedSectionId && selectedItemId) {
      updateItem(selectedSectionId, selectedItemId, { props: allValues });
    }
  }, [selectedSectionId, selectedItemId, updateItem]);

  const handleSave = useCallback(() => {
    form.validateFields().then(() => {
      message.success('Properties saved successfully!');
      onClose();
    }).catch(() => {
      message.error('Please fix the form errors before saving');
    });
  }, [form, onClose]);

  const handleDuplicate = useCallback(() => {
    if (selectedSectionId && selectedItemId) {
      duplicateItem(selectedSectionId, selectedItemId);
      message.success('Widget duplicated successfully!');
    }
  }, [selectedSectionId, selectedItemId, duplicateItem]);

  const handleDelete = useCallback(() => {
    Modal.confirm({
      title: 'Delete Widget',
      content: 'Are you sure you want to delete this widget? This action cannot be undone.',
      okText: 'Delete',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: () => {
        if (selectedSectionId && selectedItemId) {
          removeItem(selectedSectionId, selectedItemId);
          message.success('Widget deleted successfully!');
          onClose();
        }
      }
    });
  }, [selectedSectionId, selectedItemId, removeItem, onClose]);

  const handleReset = useCallback(() => {
    Modal.confirm({
      title: 'Reset Properties',
      content: 'Are you sure you want to reset all properties to default values?',
      okText: 'Reset',
      okType: 'danger',
      cancelText: 'Cancel',
      onOk: () => {
        form.resetFields();
        message.success('Properties reset to defaults!');
      }
    });
  }, [form]);

  // Enhanced widget property forms with full features
  const renderWidgetProperties = (widgetType: WidgetType, props: any) => {
    const commonFields = (
      <Card title="Basic Properties" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item name="label" label="Label" rules={[{ required: true, message: 'Label is required' }]}>
              <Input placeholder="Field label" />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item name="placeholder" label="Placeholder">
              <Input placeholder="Placeholder text" />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} sm={8}>
            <Form.Item name="required" label="Required" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item name="disabled" label="Disabled" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item name="hidden" label="Hidden" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="tooltip" label="Help Tooltip">
          <Input placeholder="Help text for users" />
        </Form.Item>
      </Card>
    );

    const validationFields = (
      <Card title="Validation Rules" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item name="minLength" label="Min Length">
              <InputNumber min={0} placeholder="Minimum characters" />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item name="maxLength" label="Max Length">
              <InputNumber min={1} placeholder="Maximum characters" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item name="pattern" label="Pattern (Regex)">
          <Input placeholder="Regular expression pattern" />
        </Form.Item>
        <Form.Item name="customErrorMessage" label="Custom Error Message">
          <Input placeholder="Error message for validation failure" />
        </Form.Item>
      </Card>
    );

    const stylingFields = (
      <Card title="Styling & Layout" size="small" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col xs={24} sm={8}>
            <Form.Item name="size" label="Size">
              <Select defaultValue="middle">
                <Option value="small">Small</Option>
                <Option value="middle">Medium</Option>
                <Option value="large">Large</Option>
              </Select>
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item name="variant" label="Variant">
              <Select defaultValue="outlined">
                <Option value="outlined">Outlined</Option>
                <Option value="filled">Filled</Option>
                <Option value="borderless">Borderless</Option>
              </Select>
            </Form.Item>
          </Col>
          <Col xs={24} sm={8}>
            <Form.Item name="status" label="Status">
              <Select>
                <Option value="">Default</Option>
                <Option value="error">Error</Option>
                <Option value="warning">Warning</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} sm={12}>
            <Form.Item name="width" label="Width">
              <Input placeholder="100%" />
            </Form.Item>
          </Col>
          <Col xs={24} sm={12}>
            <Form.Item name="margin" label="Margin">
              <Input placeholder="16px" />
            </Form.Item>
          </Col>
        </Row>
      </Card>
    );

    const specificFields = (() => {
      switch (widgetType) {
        case 'text-field':
        case 'email':
        case 'password':
          return (
            <Card title="Text Field Properties" size="small" style={{ marginBottom: 16 }}>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item name="prefix" label="Prefix">
                    <Input placeholder="Prefix text or icon" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item name="suffix" label="Suffix">
                    <Input placeholder="Suffix text or icon" />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} sm={8}>
                  <Form.Item name="allowClear" label="Allow Clear" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="showCount" label="Show Count" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="autoFocus" label="Auto Focus" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
              </Row>
            </Card>
          );

        case 'textarea':
          return (
            <Card title="Text Area Properties" size="small" style={{ marginBottom: 16 }}>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item name="rows" label="Rows">
                    <InputNumber min={1} max={20} defaultValue={4} />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item name="autoSize" label="Auto Size" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} sm={8}>
                  <Form.Item name="allowClear" label="Allow Clear" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="showCount" label="Show Count" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="resize" label="Resizable">
                    <Select defaultValue="none">
                      <Option value="none">None</Option>
                      <Option value="both">Both</Option>
                      <Option value="horizontal">Horizontal</Option>
                      <Option value="vertical">Vertical</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
            </Card>
          );

        case 'number':
          return (
            <Card title="Number Field Properties" size="small" style={{ marginBottom: 16 }}>
              <Row gutter={16}>
                <Col xs={24} sm={8}>
                  <Form.Item name="min" label="Minimum">
                    <InputNumber placeholder="Minimum value" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="max" label="Maximum">
                    <InputNumber placeholder="Maximum value" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="step" label="Step">
                    <InputNumber min={0.1} defaultValue={1} />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item name="precision" label="Decimal Places">
                    <InputNumber min={0} max={10} />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item name="formatter" label="Number Format">
                    <Select>
                      <Option value="">Default</Option>
                      <Option value="currency">Currency</Option>
                      <Option value="percentage">Percentage</Option>
                      <Option value="thousands">Thousands Separator</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} sm={8}>
                  <Form.Item name="controls" label="Show Controls" valuePropName="checked">
                    <Switch defaultChecked />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="keyboard" label="Keyboard Input" valuePropName="checked">
                    <Switch defaultChecked />
                  </Form.Item>
                </Col>
              </Row>
            </Card>
          );

        case 'select':
        case 'radio':
        case 'checkbox':
          return (
            <Card title="Options Configuration" size="small" style={{ marginBottom: 16 }}>
              <Form.Item label="Options">
                <Form.List name="options">
                  {(fields, { add, remove }) => (
                    <>
                      {fields.map(({ key, name, ...restField }) => (
                        <Card size="small" style={{ marginBottom: 8 }} key={key}>
                          <Row gutter={16} align="middle">
                            <Col xs={24} sm={8}>
                              <Form.Item {...restField} name={[name, 'label']} label="Label" style={{ margin: 0 }}>
                                <Input placeholder="Option label" />
                              </Form.Item>
                            </Col>
                            <Col xs={24} sm={8}>
                              <Form.Item {...restField} name={[name, 'value']} label="Value" style={{ margin: 0 }}>
                                <Input placeholder="Option value" />
                              </Form.Item>
                            </Col>
                            <Col xs={24} sm={6}>
                              <Form.Item {...restField} name={[name, 'disabled']} label="Disabled" valuePropName="checked" style={{ margin: 0 }}>
                                <Switch />
                              </Form.Item>
                            </Col>
                            <Col xs={24} sm={2}>
                              <Button 
                                type="text" 
                                danger 
                                icon={<DeleteOutlined />} 
                                onClick={() => remove(name)}
                                style={{ marginTop: 24 }}
                              />
                            </Col>
                          </Row>
                        </Card>
                      ))}
                      <Button 
                        type="dashed" 
                        onClick={() => add({ label: '', value: '', disabled: false })} 
                        icon={<PlusOutlined />} 
                        style={{ width: '100%' }}
                      >
                        Add Option
                      </Button>
                    </>
                  )}
                </Form.List>
              </Form.Item>
              {widgetType === 'select' && (
                <>
                  <Row gutter={16}>
                    <Col xs={24} sm={8}>
                      <Form.Item name="allowClear" label="Allow Clear" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col xs={24} sm={8}>
                      <Form.Item name="showSearch" label="Searchable" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col xs={24} sm={8}>
                      <Form.Item name="multiple" label="Multiple" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item name="maxTagCount" label="Max Tags (Multiple)">
                    <InputNumber min={1} placeholder="Maximum visible tags" />
                  </Form.Item>
                </>
              )}
            </Card>
          );

        case 'upload':
          return (
            <Card title="Upload Configuration" size="small" style={{ marginBottom: 16 }}>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item name="accept" label="Accept File Types">
                    <Input placeholder="image/*,.pdf,.doc" />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item name="maxCount" label="Max Files">
                    <InputNumber min={1} defaultValue={1} />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} sm={12}>
                  <Form.Item name="maxSize" label="Max Size (MB)">
                    <InputNumber min={0.1} defaultValue={10} />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={12}>
                  <Form.Item name="listType" label="Display Type">
                    <Select defaultValue="text">
                      <Option value="text">Text</Option>
                      <Option value="picture">Picture</Option>
                      <Option value="picture-card">Picture Card</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col xs={24} sm={8}>
                  <Form.Item name="multiple" label="Multiple" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="directory" label="Folder Upload" valuePropName="checked">
                    <Switch />
                  </Form.Item>
                </Col>
                <Col xs={24} sm={8}>
                  <Form.Item name="showUploadList" label="Show List" valuePropName="checked">
                    <Switch defaultChecked />
                  </Form.Item>
                </Col>
              </Row>
            </Card>
          );

        default:
          return (
            <Card title="Widget Properties" size="small" style={{ marginBottom: 16 }}>
              <Empty description="No specific properties available for this widget type" />
            </Card>
          );
      }
    })();

    return (
      <Form
        form={form}
        layout="vertical"
        initialValues={props}
        onValuesChange={handleWidgetPropsChange}
        size="middle"
      >
        {commonFields}
        {validationFields}
        {stylingFields}
        {specificFields}
      </Form>
    );
  };

  // Preview component
  const renderPreview = () => {
    if (!selectedItem) return null;

    return (
      <Card title="Widget Preview" style={{ marginBottom: 16 }}>
        <div style={{ 
          padding: 24, 
          background: '#f8f9fa', 
          borderRadius: 8,
          border: '2px dashed #e9ecef'
        }}>
          <WidgetRenderer
            widget={{
              id: selectedItem.id,
              type: selectedItem.type,
              props: selectedItem.props
            }}
            mode="preview"
          />
        </div>
      </Card>
    );
  };

  if (!selectedItem) {
    return (
      <Layout style={{ height: '100vh' }}>
        <Header style={{ 
          background: 'white', 
          borderBottom: '1px solid #f0f0f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={onClose}>
              Back to Builder
            </Button>
            <Title level={4} style={{ margin: 0 }}>
              Property Editor
            </Title>
          </Space>
        </Header>
        <Content style={{ padding: 24, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Empty 
            description="No widget selected" 
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" onClick={onClose}>
              Go Back to Builder
            </Button>
          </Empty>
        </Content>
      </Layout>
    );
  }

  return (
    <Layout style={{ height: '100vh' }}>
      <Header style={{ 
        background: 'white', 
        borderBottom: '1px solid #f0f0f0',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <Space>
          <Button icon={<ArrowLeftOutlined />} onClick={onClose}>
            Back
          </Button>
          <Title level={4} style={{ margin: 0 }}>
            Configure Widget: {selectedItem.type}
          </Title>
          <Badge count={Object.keys(selectedItem.props || {}).length} showZero>
            <Tag color="blue">Properties</Tag>
          </Badge>
        </Space>
        
        <Space>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            Reset
          </Button>
          <Button icon={<CopyOutlined />} onClick={handleDuplicate}>
            Duplicate
          </Button>
          <Button danger icon={<DeleteOutlined />} onClick={handleDelete}>
            Delete
          </Button>
          <Button type="primary" icon={<SaveOutlined />} onClick={handleSave}>
            Save & Close
          </Button>
        </Space>
      </Header>

      <Content style={{ padding: 24, overflow: 'auto' }}>
        <Row gutter={24}>
          <Col xs={24} lg={16}>
            <Tabs activeKey={activeTab} onChange={setActiveTab}>
              <TabPane 
                tab={<span><SettingOutlined />Properties</span>} 
                key="properties"
              >
                {renderWidgetProperties(selectedItem.type, selectedItem.props)}
              </TabPane>
              <TabPane 
                tab={<span><InfoCircleOutlined />Documentation</span>} 
                key="docs"
              >
                <Card title="Widget Documentation" style={{ marginBottom: 16 }}>
                  <Paragraph>
                    <strong>Widget Type:</strong> {selectedItem.type}
                  </Paragraph>
                  <Paragraph>
                    <strong>Description:</strong> Detailed documentation and usage examples will be shown here.
                  </Paragraph>
                  <Paragraph>
                    <strong>Common Use Cases:</strong>
                    <ul>
                      <li>Form data collection</li>
                      <li>User input validation</li>
                      <li>Interactive form elements</li>
                    </ul>
                  </Paragraph>
                </Card>
              </TabPane>
            </Tabs>
          </Col>
          <Col xs={24} lg={8}>
            {renderPreview()}
            
            <Card title="Widget Actions" style={{ marginBottom: 16 }}>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Button 
                  block 
                  icon={<CopyOutlined />} 
                  onClick={handleDuplicate}
                >
                  Duplicate Widget
                </Button>
                <Button 
                  block 
                  danger 
                  icon={<DeleteOutlined />} 
                  onClick={handleDelete}
                >
                  Delete Widget
                </Button>
              </Space>
            </Card>
            
            <Card title="Quick Info" size="small">
              <Space direction="vertical" size={4}>
                <Text type="secondary">Widget ID: {selectedItem.id}</Text>
                <Text type="secondary">Type: {selectedItem.type}</Text>
                <Text type="secondary">Section: {selectedSection?.title}</Text>
              </Space>
            </Card>
          </Col>
        </Row>
      </Content>
    </Layout>
  );
};

export default PropertyEditor;
