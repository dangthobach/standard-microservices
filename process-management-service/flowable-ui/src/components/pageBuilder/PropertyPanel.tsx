import React, { useCallback, useMemo } from 'react';
import { 
  Typography, 
  Form, 
  Input, 
  InputNumber, 
  Select, 
  Switch, 
  Button, 
  Space, 
  Divider, 
  ColorPicker,
  Slider
} from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { usePageBuilderStore } from '../../store/pageBuilderStore';
import { WidgetType, GridSection, StackSection } from '../../types/pageBuilder';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

interface PropertyPanelProps {
  collapsed?: boolean;
}

const PropertyPanel: React.FC<PropertyPanelProps> = ({ collapsed = false }) => {
  const {
    schema,
    selectedItemId,
    selectedSectionId,
    updateItem,
    updateSection,
    setSchema
  } = usePageBuilderStore();

  const [form] = Form.useForm();

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

  const handleSectionChange = useCallback((changedValues: any, allValues: any) => {
    if (selectedSectionId) {
      updateSection(selectedSectionId, allValues);
    }
  }, [selectedSectionId, updateSection]);

  const handleSchemaChange = useCallback((changedValues: any, allValues: any) => {
    setSchema({ ...schema, ...allValues });
  }, [schema, setSchema]);

  // Widget property forms based on widget type
  const renderWidgetProperties = (widgetType: WidgetType, props: any) => {
    const commonFields = (
      <>
        <Form.Item name="label" label={<span style={{fontSize: 12, fontWeight: 500}}>Label</span>} style={{ marginBottom: 16 }}>
          <Input placeholder="Field label" />
        </Form.Item>
        <Form.Item name="placeholder" label={<span style={{fontSize: 12, fontWeight: 500}}>Placeholder</span>} style={{ marginBottom: 16 }}>
          <Input placeholder="Placeholder text" />
        </Form.Item>
        <Form.Item name="required" label={<span style={{fontSize: 12, fontWeight: 500}}>Required</span>} valuePropName="checked" style={{ marginBottom: 16 }}>
          <Switch />
        </Form.Item>
        <Form.Item name="disabled" label={<span style={{fontSize: 12, fontWeight: 500}}>Disabled</span>} valuePropName="checked" style={{ marginBottom: 16 }}>
          <Switch />
        </Form.Item>
      </>
    );

    const specificFields = (() => {
      switch (widgetType) {
        case 'text-field':
        case 'email':
        case 'password':
          return (
            <>
              <Form.Item name="maxLength" label={<span style={{fontSize: 12, fontWeight: 500}}>Max Length</span>} style={{ marginBottom: 16 }}>
                <InputNumber min={1} placeholder="Maximum characters" style={{ width: '100%' }} />
              </Form.Item>
            </>
          );

        case 'textarea':
          return (
            <>
              <Form.Item name="rows" label={<span style={{fontSize: 12, fontWeight: 500}}>Rows</span>} style={{ marginBottom: 16 }}>
                <InputNumber min={1} max={20} defaultValue={4} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item name="maxLength" label={<span style={{fontSize: 12, fontWeight: 500}}>Max Length</span>} style={{ marginBottom: 16 }}>
                <InputNumber min={1} placeholder="Maximum characters" style={{ width: '100%' }} />
              </Form.Item>
            </>
          );

        case 'number':
          return (
            <>
              <Form.Item name="min" label={<span style={{fontSize: 12, fontWeight: 500}}>Minimum</span>} style={{ marginBottom: 16 }}>
                <InputNumber placeholder="Minimum value" style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item name="max" label={<span style={{fontSize: 12, fontWeight: 500}}>Maximum</span>} style={{ marginBottom: 16 }}>
                <InputNumber placeholder="Maximum value" style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item name="step" label={<span style={{fontSize: 12, fontWeight: 500}}>Step</span>} style={{ marginBottom: 16 }}>
                <InputNumber min={0.1} defaultValue={1} style={{ width: '100%' }} />
              </Form.Item>
            </>
          );

        case 'select':
        case 'radio':
        case 'checkbox':
          return (
            <>
              <Form.Item label={<span style={{fontSize: 12, fontWeight: 500}}>Options</span>} style={{ marginBottom: 16 }}>
                <Form.List name="options">
                  {(fields, { add, remove }) => (
                    <>
                      {fields.map(({ key, name, ...restField }) => (
                        <Space key={key} style={{ display: 'flex', marginBottom: 8, width: '100%' }}>
                          <Form.Item {...restField} name={[name, 'label']} style={{ margin: 0, flex: 1 }}>
                            <Input placeholder="Label" />
                          </Form.Item>
                          <Form.Item {...restField} name={[name, 'value']} style={{ margin: 0, flex: 1 }}>
                            <Input placeholder="Value" />
                          </Form.Item>
                          <Button 
                            type="text" 
                            danger 
                            icon={<DeleteOutlined />} 
                            onClick={() => remove(name)} 
                            style={{ flexShrink: 0 }}
                          />
                        </Space>
                      ))}
                      <Button 
                        type="dashed" 
                        onClick={() => add()} 
                        icon={<PlusOutlined />} 
                        style={{ width: '100%', marginTop: 8 }}
                      >
                        Add Option
                      </Button>
                    </>
                  )}
                </Form.List>
              </Form.Item>
            </>
          );

        default:
          return null;
      }
    })();

    return (
      <Form
        form={form}
        layout="vertical"
        initialValues={props}
        onValuesChange={handleWidgetPropsChange}
      >
        {/* Basic Properties Section */}
        <div style={{
          marginBottom: 24,
          padding: '12px 16px',
          background: '#f8f9fa',
          borderRadius: 6,
          border: '1px solid #e9ecef'
        }}>
          <div style={{
            fontSize: 13,
            fontWeight: 600,
            color: '#495057',
            marginBottom: 12
          }}>
            Basic Properties
          </div>
          {commonFields}
        </div>

        {/* Specific Properties Section */}
        {specificFields && (
          <div style={{
            marginBottom: 16,
            padding: '12px 16px',
            background: '#f8f9fa',
            borderRadius: 6,
            border: '1px solid #e9ecef'
          }}>
            <div style={{
              fontSize: 13,
              fontWeight: 600,
              color: '#495057',
              marginBottom: 12
            }}>
              Advanced Properties
            </div>
            {specificFields}
          </div>
        )}

        {/* Styling Section */}
        <div style={{
          padding: '12px 16px',
          background: '#f0f8ff',
          borderRadius: 6,
          border: '1px solid #d4edda'
        }}>
          <div style={{
            fontSize: 13,
            fontWeight: 600,
            color: '#1890ff',
            marginBottom: 12
          }}>
            Styling & Layout
          </div>
          <Form.Item name="width" label={<span style={{fontSize: 12, fontWeight: 500}}>Width</span>} style={{ marginBottom: 16 }}>
            <Select placeholder="Select width" allowClear>
              <Option value="25%">25%</Option>
              <Option value="50%">50%</Option>
              <Option value="75%">75%</Option>
              <Option value="100%">100%</Option>
            </Select>
          </Form.Item>
          <Form.Item name="margin" label={<span style={{fontSize: 12, fontWeight: 500}}>Margin</span>} style={{ marginBottom: 0 }}>
            <Select placeholder="Select margin" allowClear>
              <Option value="small">Small</Option>
              <Option value="medium">Medium</Option>
              <Option value="large">Large</Option>
            </Select>
          </Form.Item>
        </div>
      </Form>
    );
  };

  // Section property forms
  const renderSectionProperties = (section: GridSection | StackSection) => {
    if (section.type === 'grid') {
      return (
        <Form
          layout="vertical"
          initialValues={section}
          onValuesChange={handleSectionChange}
          size="small"
        >
          <Form.Item name="title" label="Section Title">
            <Input placeholder="Section title" />
          </Form.Item>
          <Form.Item name="cols" label="Columns">
            <InputNumber min={1} max={24} />
          </Form.Item>
          <Form.Item name="rowHeight" label="Row Height">
            <InputNumber min={30} max={200} />
          </Form.Item>
        </Form>
      );
    } else if (section.type === 'stack') {
      return (
        <Form
          layout="vertical"
          initialValues={section}
          onValuesChange={handleSectionChange}
          size="small"
        >
          <Form.Item name="title" label="Section Title">
            <Input placeholder="Section title" />
          </Form.Item>
          <Form.Item name="direction" label="Direction">
            <Select>
              <Option value="row">Row</Option>
              <Option value="column">Column</Option>
            </Select>
          </Form.Item>
          <Form.Item name="align" label="Align Items">
            <Select>
              <Option value="start">Start</Option>
              <Option value="center">Center</Option>
              <Option value="end">End</Option>
              <Option value="stretch">Stretch</Option>
            </Select>
          </Form.Item>
          <Form.Item name="justify" label="Justify Content">
            <Select>
              <Option value="start">Start</Option>
              <Option value="center">Center</Option>
              <Option value="end">End</Option>
              <Option value="space-between">Space Between</Option>
              <Option value="space-around">Space Around</Option>
            </Select>
          </Form.Item>
          <Form.Item name="gap" label="Gap">
            <InputNumber min={0} max={100} />
          </Form.Item>
          <Form.Item name="wrap" label="Wrap" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      );
    }
    return null;
  };

  // Schema/Form properties
  const renderSchemaProperties = () => (
    <Form
      layout="vertical"
      initialValues={schema}
      onValuesChange={handleSchemaChange}
      size="small"
    >
      <Form.Item name="title" label="Form Title">
        <Input placeholder="Form title" />
      </Form.Item>
      <Form.Item name="description" label="Description">
        <TextArea rows={3} placeholder="Form description" />
      </Form.Item>
      <Form.Item name="layout" label="Layout Type">
        <Select>
          <Option value="classic">Classic</Option>
          <Option value="card">Card</Option>
        </Select>
      </Form.Item>
      <Divider />
      <Form.Item name={['theme', 'primaryColor']} label="Primary Color">
        <ColorPicker />
      </Form.Item>
      <Form.Item name={['theme', 'borderRadius']} label="Border Radius">
        <Slider min={0} max={20} />
      </Form.Item>
      <Form.Item name={['theme', 'spacing']} label="Spacing">
        <Slider min={8} max={32} />
      </Form.Item>
      <Divider />
      <Form.Item name={['settings', 'showProgress']} label="Show Progress" valuePropName="checked">
        <Switch />
      </Form.Item>
      <Form.Item name={['settings', 'allowBack']} label="Allow Back" valuePropName="checked">
        <Switch />
      </Form.Item>
      <Form.Item name={['settings', 'autoSave']} label="Auto Save" valuePropName="checked">
        <Switch />
      </Form.Item>
    </Form>
  );

  if (collapsed) {
    return (
      <div style={{ 
        padding: 12, 
        textAlign: 'center',
        height: '100vh',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'flex-start',
        paddingTop: 24
      }}>
        <Title level={5} style={{ 
          writingMode: 'vertical-rl', 
          textOrientation: 'mixed',
          margin: 0,
          color: '#1890ff'
        }}>
          Properties
        </Title>
        {selectedItem && (
          <div style={{ 
            marginTop: 16, 
            padding: 8,
            background: '#f0f8ff',
            borderRadius: 4,
            border: '1px solid #d4edda'
          }}>
            <Text style={{ 
              fontSize: 10, 
              color: '#1890ff',
              writingMode: 'vertical-rl',
              textOrientation: 'mixed'
            }}>
              {selectedItem.type}
            </Text>
          </div>
        )}
        <Text type="secondary" style={{ 
          fontSize: 10, 
          marginTop: 16,
          writingMode: 'vertical-rl',
          textOrientation: 'mixed'
        }}>
          Expand to edit
        </Text>
      </div>
    );
  }

  return (
    <div style={{ 
      height: '100vh', 
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
      background: '#fafafa'
    }}>
      <div style={{
        padding: '16px 20px',
        borderBottom: '1px solid #e8e8e8',
        background: 'white',
        flexShrink: 0
      }}>
        <div style={{ 
          fontWeight: 600, 
          fontSize: 16,
          color: '#262626',
          marginBottom: 4
        }}>
          Properties
        </div>
        <div style={{
          fontSize: 12,
          color: '#8c8c8c'
        }}>
          {selectedItem ? `${selectedItem.type} settings` : selectedSection ? `${selectedSection.type} section` : 'Form configuration'}
        </div>
      </div>

      <div style={{ 
        flex: 1, 
        overflow: 'auto',
        padding: '16px 20px',
        background: '#fafafa'
      }}>
        {selectedItem ? (
          <div style={{
            background: 'white',
            borderRadius: 8,
            border: '1px solid #e8e8e8',
            overflow: 'hidden'
          }}>
            {/* Widget ID and Type Header */}
            <div style={{
              padding: '12px 16px',
              borderBottom: '1px solid #f0f0f0',
              background: '#fafafa'
            }}>
              <div style={{ 
                fontSize: 13, 
                fontWeight: 600, 
                color: '#1890ff',
                marginBottom: 2
              }}>
                ID: {selectedItem.id}
              </div>
              <div style={{ 
                fontSize: 11, 
                color: '#8c8c8c',
                textTransform: 'capitalize'
              }}>
                {selectedItem.type.replace('-', ' ')} Widget
              </div>
            </div>

            {/* Properties Form */}
            <div style={{ padding: '16px' }}>
              {renderWidgetProperties(selectedItem.type, selectedItem.props)}
            </div>
          </div>
        ) : selectedSection ? (
          <div style={{
            background: 'white',
            borderRadius: 8,
            border: '1px solid #e8e8e8',
            padding: 16
          }}>
            <div style={{
              marginBottom: 16,
              padding: '8px 12px',
              background: '#f0f8ff',
              borderRadius: 6,
              border: '1px solid #d4edda'
            }}>
              <Text style={{ fontSize: 13, color: '#1890ff', fontWeight: 500 }}>
                Section: {selectedSection.type}
              </Text>
            </div>
            {renderSectionProperties(selectedSection)}
          </div>
        ) : (
          <div style={{
            background: 'white',
            borderRadius: 8,
            border: '1px solid #e8e8e8',
            padding: 16
          }}>
            {renderSchemaProperties()}
            <div style={{
              marginTop: 24,
              padding: 16,
              textAlign: 'center',
              color: '#8c8c8c'
            }}>
              <div style={{ fontSize: 13, marginBottom: 8 }}>
                💡 Select any widget to edit its properties
              </div>
              <div style={{ fontSize: 11 }}>
                Click on widgets in the canvas to configure them
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default PropertyPanel;
