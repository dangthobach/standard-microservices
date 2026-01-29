import React from 'react';
import { Typography, Row, Col, Button, Space, Divider, Tooltip } from 'antd';
import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import {
  FormOutlined,
  NumberOutlined,
  MailOutlined,
  LockOutlined,
  CaretDownOutlined,
  CheckCircleOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  SettingOutlined,
  UploadOutlined,
  NodeIndexOutlined,
  BranchesOutlined,
  SwapOutlined,
  FileImageOutlined,
  PlayCircleOutlined,
  FontSizeOutlined,
  MinusOutlined,
  LayoutOutlined,
  CreditCardOutlined,
  TagsOutlined,
  MenuUnfoldOutlined,
  StepForwardOutlined,
  UnorderedListOutlined,
  TableOutlined,
  BarChartOutlined,
  PlusOutlined,
  PhoneOutlined,
  GlobalOutlined,
  CodeOutlined,
  BgColorsOutlined,
  HighlightOutlined,
  FileTextOutlined,
  DollarOutlined,
  PercentageOutlined,
  QrcodeOutlined,
  ScanOutlined,
  AppstoreOutlined
} from '@ant-design/icons';
import { WidgetType } from '../../types/pageBuilder';
import { usePageBuilderStore } from '../../store/pageBuilderStore';

const { Text } = Typography;

interface WidgetConfig {
  type: WidgetType;
  label: string;
  icon: React.ReactNode;
  category: string;
  description?: string;
}

const WIDGET_CATEGORIES = [
  {
    key: 'basic-input',
    label: 'Basic Input',
    widgets: [
      { 
        type: 'text-field', 
        label: 'Text field', 
        icon: <FormOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Single line text input field'
      },
      { 
        type: 'textarea', 
        label: 'Text area', 
        icon: <FileTextOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Multi-line text input area'
      },
      { 
        type: 'number', 
        label: 'Number', 
        icon: <NumberOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Numeric input field'
      },
      { 
        type: 'email', 
        label: 'Email', 
        icon: <MailOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Email address input'
      },
      { 
        type: 'password', 
        label: 'Password', 
        icon: <LockOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Password input field'
      },
      { 
        type: 'phone', 
        label: 'Phone', 
        icon: <PhoneOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Phone number input'
      },
      { 
        type: 'url', 
        label: 'URL', 
        icon: <GlobalOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Website URL input'
      },
      { 
        type: 'currency', 
        label: 'Currency', 
        icon: <DollarOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Currency amount input'
      },
      { 
        type: 'percentage', 
        label: 'Percentage', 
        icon: <PercentageOutlined style={{ fontSize: '18px' }} />, 
        category: 'basic-input', 
        description: 'Percentage value input'
      }
    ] as WidgetConfig[]
  },
  {
    key: 'selection',
    label: 'Selection',
    widgets: [
      { 
        type: 'checkbox', 
        label: 'Checkbox', 
        icon: <CheckCircleOutlined style={{ fontSize: '18px' }} />, 
        category: 'selection', 
        description: 'Multiple choice checkbox'
      },
      { 
        type: 'radio', 
        label: 'Radio', 
        icon: <CheckCircleOutlined style={{ fontSize: '18px', color: '#1890ff' }} />, 
        category: 'selection', 
        description: 'Single choice radio buttons'
      },
      { 
        type: 'select', 
        label: 'Select', 
        icon: <CaretDownOutlined style={{ fontSize: '18px' }} />, 
        category: 'selection', 
        description: 'Dropdown selection'
      },
      { 
        type: 'switch', 
        label: 'Switch', 
        icon: <SettingOutlined style={{ fontSize: '18px' }} />, 
        category: 'selection', 
        description: 'Toggle switch control'
      },
      { 
        type: 'cascader', 
        label: 'Cascader', 
        icon: <NodeIndexOutlined style={{ fontSize: '18px' }} />, 
        category: 'selection', 
        description: 'Hierarchical selection'
      },
      { 
        type: 'tree-select', 
        label: 'Tree Select', 
        icon: <BranchesOutlined style={{ fontSize: '18px' }} />, 
        category: 'selection', 
        description: 'Tree structure selection'
      },
      { 
        type: 'transfer', 
        label: 'Transfer', 
        icon: <SwapOutlined style={{ fontSize: '18px' }} />, 
        category: 'selection', 
        description: 'Dual list transfer'
      }
    ] as WidgetConfig[]
  },
  {
    key: 'datetime',
    label: 'Date & Time',
    widgets: [
      { 
        type: 'date', 
        label: 'Date time', 
        icon: <CalendarOutlined style={{ fontSize: '18px' }} />, 
        category: 'datetime', 
        description: 'Date picker input'
      },
      { 
        type: 'time', 
        label: 'Time', 
        icon: <ClockCircleOutlined style={{ fontSize: '18px' }} />, 
        category: 'datetime', 
        description: 'Time picker input'
      },
      { 
        type: 'calendar', 
        label: 'Calendar', 
        icon: <CalendarOutlined style={{ fontSize: '18px', color: '#52c41a' }} />, 
        category: 'datetime', 
        description: 'Full calendar view'
      }
    ] as WidgetConfig[]
  },
  {
    key: 'presentation',
    label: 'Presentation',
    widgets: [
      { 
        type: 'divider', 
        label: 'Divider', 
        icon: <MinusOutlined style={{ fontSize: '18px' }} />, 
        category: 'presentation', 
        description: 'Visual separator line'
      },
      { 
        type: 'text-block', 
        label: 'Text block', 
        icon: <FontSizeOutlined style={{ fontSize: '18px' }} />, 
        category: 'presentation', 
        description: 'Static text display'
      },
      { 
        type: 'image', 
        label: 'Image view', 
        icon: <FileImageOutlined style={{ fontSize: '18px' }} />, 
        category: 'presentation', 
        description: 'Image display component'
      },
      { 
        type: 'video', 
        label: 'Video', 
        icon: <PlayCircleOutlined style={{ fontSize: '18px' }} />, 
        category: 'presentation', 
        description: 'Video player component'
      },
      { 
        type: 'html', 
        label: 'HTML', 
        icon: <CodeOutlined style={{ fontSize: '18px' }} />, 
        category: 'presentation', 
        description: 'Custom HTML content'
      },
      { 
        type: 'steps', 
        label: 'Steps', 
        icon: <StepForwardOutlined style={{ fontSize: '18px' }} />, 
        category: 'presentation', 
        description: 'Step progress indicator'
      }
    ] as WidgetConfig[]
  },
  {
    key: 'advanced',
    label: 'Advanced',
    widgets: [
      { 
        type: 'upload', 
        label: 'File Upload', 
        icon: <UploadOutlined style={{ fontSize: '18px' }} />, 
        category: 'advanced', 
        description: 'File upload component'
      },
      { 
        type: 'signature', 
        label: 'Signature', 
        icon: <HighlightOutlined style={{ fontSize: '18px' }} />, 
        category: 'advanced', 
        description: 'Digital signature pad'
      },
      { 
        type: 'qrcode', 
        label: 'QR Code', 
        icon: <QrcodeOutlined style={{ fontSize: '18px' }} />, 
        category: 'advanced', 
        description: 'QR code generator'
      },
      { 
        type: 'barcode', 
        label: 'Barcode', 
        icon: <ScanOutlined style={{ fontSize: '18px' }} />, 
        category: 'advanced', 
        description: 'Barcode generator'
      },
      { 
        type: 'color-picker', 
        label: 'Color Picker', 
        icon: <BgColorsOutlined style={{ fontSize: '18px' }} />, 
        category: 'advanced', 
        description: 'Color selection tool'
      },
      { 
        type: 'code-editor', 
        label: 'Code Editor', 
        icon: <CodeOutlined style={{ fontSize: '18px', color: '#722ed1' }} />, 
        category: 'advanced', 
        description: 'Code editing component'
      }
    ] as WidgetConfig[]
  },
  {
    key: 'layout',
    label: 'Layout',
    widgets: [
      { 
        type: 'card', 
        label: 'Card', 
        icon: <CreditCardOutlined style={{ fontSize: '18px' }} />, 
        category: 'layout', 
        description: 'Card container'
      },
      { 
        type: 'tabs', 
        label: 'Tabs', 
        icon: <TagsOutlined style={{ fontSize: '18px' }} />, 
        category: 'layout', 
        description: 'Tabbed content'
      },
      { 
        type: 'collapse', 
        label: 'Collapse', 
        icon: <MenuUnfoldOutlined style={{ fontSize: '18px' }} />, 
        category: 'layout', 
        description: 'Collapsible panels'
      },
      { 
        type: 'space', 
        label: 'Space', 
        icon: <LayoutOutlined style={{ fontSize: '18px' }} />, 
        category: 'layout', 
        description: 'Spacing component'
      }
    ] as WidgetConfig[]
  },
  {
    key: 'data-display',
    label: 'Data Display',
    widgets: [
      { 
        type: 'table', 
        label: 'Table', 
        icon: <TableOutlined style={{ fontSize: '18px' }} />, 
        category: 'data-display', 
        description: 'Data table'
      },
      { 
        type: 'list', 
        label: 'List', 
        icon: <UnorderedListOutlined style={{ fontSize: '18px' }} />, 
        category: 'data-display', 
        description: 'Data list'
      },
      { 
        type: 'chart', 
        label: 'Chart', 
        icon: <BarChartOutlined style={{ fontSize: '18px' }} />, 
        category: 'data-display', 
        description: 'Data visualization'
      },
      { 
        type: 'timeline', 
        label: 'Timeline', 
        icon: <StepForwardOutlined style={{ fontSize: '18px', color: '#13c2c2' }} />, 
        category: 'data-display', 
        description: 'Timeline display'
      }
    ] as WidgetConfig[]
  }
];

interface DraggableWidgetProps {
  widget: WidgetConfig;
  compact?: boolean;
}

const DraggableWidget: React.FC<DraggableWidgetProps> = ({ widget, compact = false }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    isDragging,
  } = useDraggable({
    id: `widget-${widget.type}`,
    data: {
      type: 'widget',
      widgetType: widget.type,
      widget
    },
  });

  const style = {
    transform: CSS.Translate.toString(transform),
    opacity: isDragging ? 0.5 : 1,
  };

  // Compact mode for collapsed sidebar
  if (compact) {
    return (
      <Tooltip title={widget.label} placement="right">
        <div
          ref={setNodeRef}
          style={style}
          {...listeners}
          {...attributes}
          className="widget-item"
        >
          <div style={{
            width: 36,
            height: 36,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: isDragging ? '#e6f7ff' : '#f8f9fa',
            border: isDragging ? '2px solid #1890ff' : '1px solid #d9d9d9',
            borderRadius: 6,
            cursor: isDragging ? 'grabbing' : 'grab',
            transition: 'all 0.2s ease',
            fontSize: 16,
            color: isDragging ? '#1890ff' : '#666',
            margin: '0 auto 4px auto'
          }}
          onMouseEnter={(e) => {
            if (!isDragging) {
              e.currentTarget.style.background = '#e6f7ff';
              e.currentTarget.style.borderColor = '#1890ff';
              e.currentTarget.style.color = '#1890ff';
              e.currentTarget.style.transform = 'scale(1.05)';
            }
          }}
          onMouseLeave={(e) => {
            if (!isDragging) {
              e.currentTarget.style.background = '#f8f9fa';
              e.currentTarget.style.borderColor = '#d9d9d9';
              e.currentTarget.style.color = '#666';
              e.currentTarget.style.transform = 'scale(1)';
            }
          }}
          >
            {widget.icon}
          </div>
        </div>
      </Tooltip>
    );
  }

  // Regular mode for expanded sidebar
  return (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      className="widget-item"
    >
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: '12px 8px',
        background: isDragging ? '#e6f7ff' : 'white',
        border: isDragging ? '1px solid #1890ff' : '1px solid #e8e8e8',
        borderRadius: 6,
        cursor: isDragging ? 'grabbing' : 'grab',
        transition: 'all 0.2s ease',
        marginBottom: 8,
        minHeight: 72,
        width: '100%',
        textAlign: 'center'
      }}
      onMouseEnter={(e) => {
        if (!isDragging) {
          e.currentTarget.style.background = '#f0f9ff';
          e.currentTarget.style.borderColor = '#91caff';
          e.currentTarget.style.boxShadow = '0 2px 8px rgba(24, 144, 255, 0.12)';
          e.currentTarget.style.transform = 'translateY(-1px)';
        }
      }}
      onMouseLeave={(e) => {
        if (!isDragging) {
          e.currentTarget.style.background = 'white';
          e.currentTarget.style.borderColor = '#e8e8e8';
          e.currentTarget.style.boxShadow = 'none';
          e.currentTarget.style.transform = 'translateY(0px)';
        }
      }}
      >
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 32,
          height: 32,
          marginBottom: 8,
          background: '#fafafa',
          borderRadius: 4,
          color: '#595959',
          fontSize: 16,
          flexShrink: 0
        }}>
          {widget.icon}
        </div>
        
        <div style={{
          fontSize: '12px',
          fontWeight: 500,
          color: '#262626',
          lineHeight: '14px',
          textAlign: 'center',
          wordBreak: 'break-word',
          overflow: 'hidden',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          maxWidth: '100%'
        }}>
          {widget.label}
        </div>
      </div>
    </div>
  );
};

interface WidgetPaletteProps {
  collapsed?: boolean;
}

const WidgetPalette: React.FC<WidgetPaletteProps> = ({ collapsed = false }) => {
  const { addGridSection, addStackSection, schema } = usePageBuilderStore();

  const handleAddGridSection = () => {
    addGridSection('New Grid Section', 12);
  };

  const handleAddStackSection = () => {
    addStackSection('New Stack Section', 'column');
  };

  if (collapsed) {
    return (
      <div style={{ 
        padding: 4, 
        textAlign: 'center',
        height: '100vh',
        overflow: 'auto',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center'
      }}>
        {/* Collapsed Header */}
        <div style={{
          padding: '8px 4px',
          borderBottom: '1px solid #f0f0f0',
          marginBottom: 8,
          width: '100%'
        }}>
          <div style={{
            width: 32,
            height: 32,
            borderRadius: 6,
            background: '#1890ff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto',
            marginBottom: 4
          }}>
            <span style={{ color: 'white', fontSize: 14, fontWeight: 'bold' }}>W</span>
          </div>
          <Text style={{ 
            fontSize: 10, 
            color: '#1890ff',
            fontWeight: 600,
            display: 'block'
          }}>
            Widgets
          </Text>
        </div>

        {/* Compact Section Buttons */}
        <div style={{ marginBottom: 12, width: '100%', padding: '0 4px' }}>
          <Tooltip title="Add Grid Section" placement="right">
            <Button
              size="small"
              icon={<PlusOutlined style={{ fontSize: 12 }} />}
              onClick={handleAddGridSection}
              style={{
                width: '100%',
                height: 24,
                marginBottom: 4,
                fontSize: 10,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderColor: '#1890ff',
                color: '#1890ff'
              }}
            />
          </Tooltip>
          <Tooltip title="Add Stack Section" placement="right">
            <Button
              size="small"
              icon={<AppstoreOutlined style={{ fontSize: 12 }} />}
              onClick={handleAddStackSection}
              style={{
                width: '100%',
                height: 24,
                fontSize: 10,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderColor: '#1890ff',
                color: '#1890ff'
              }}
            />
          </Tooltip>
        </div>

        {/* Most Used Widgets - Icon Only */}
        <div style={{ 
          display: 'flex', 
          flexDirection: 'column', 
          gap: 4,
          width: '100%',
          padding: '0 4px',
          flex: 1
        }}>
          <Text style={{
            fontSize: 9,
            color: '#666',
            textAlign: 'center',
            marginBottom: 4,
            paddingBottom: 4,
            borderBottom: '1px solid #f0f0f0'
          }}>
            Most Used
          </Text>
          {[
            { type: 'text-field', icon: <FormOutlined />, tooltip: 'Text Field' },
            { type: 'textarea', icon: <FormOutlined />, tooltip: 'Text Area' },
            { type: 'number', icon: <NumberOutlined />, tooltip: 'Number' },
            { type: 'email', icon: <MailOutlined />, tooltip: 'Email' },
            { type: 'select', icon: <CaretDownOutlined />, tooltip: 'Select' },
            { type: 'radio', icon: <CheckCircleOutlined />, tooltip: 'Radio Group' },
            { type: 'checkbox', icon: <CheckCircleOutlined />, tooltip: 'Checkbox' },
            { type: 'date', icon: <CalendarOutlined />, tooltip: 'Date Picker' }
          ].map(widget => {
            const widgetConfig = WIDGET_CATEGORIES.flatMap(cat => cat.widgets).find(w => w.type === widget.type);
            if (!widgetConfig) return null;
            
            return (
              <div key={widget.type}>
                <DraggableWidget 
                  widget={widgetConfig} 
                  compact={true}
                />
              </div>
            );
          })}
        </div>

        {/* Expand Hint */}
        <div style={{ 
          marginTop: 'auto',
          padding: '8px 4px',
          borderTop: '1px solid #f0f0f0',
          width: '100%'
        }}>
          <Text style={{ 
            fontSize: 9, 
            color: '#999',
            display: 'block',
            textAlign: 'center'
          }}>
            Click to expand
          </Text>
        </div>
      </div>
    );
  }

  return (
    <div style={{ 
      padding: 16, 
      height: '100%', 
      overflow: 'auto',
      scrollbarWidth: 'thin',
      scrollbarColor: '#d9d9d9 #f0f0f0'
    }}>
      <div style={{
        padding: '20px 16px 16px 16px',
        background: 'white',
        borderBottom: '1px solid #e8e8e8'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          marginBottom: 4
        }}>
          <div style={{
            width: 3,
            height: 20,
            background: '#1890ff',
            borderRadius: 2,
            marginRight: 8
          }} />
          <span style={{ 
            fontSize: 16, 
            fontWeight: 600, 
            color: '#262626' 
          }}>
            Widget Palette
          </span>
        </div>
        <div style={{
          fontSize: 12,
          color: '#8c8c8c',
          marginLeft: 11
        }}>
          Drag widgets to build your form
        </div>
      </div>
      
      {/* Section Controls */}
      <div style={{ 
        margin: '16px',
        padding: '16px',
        background: 'white',
        borderRadius: 8,
        border: '1px solid #e8e8e8',
        marginBottom: 16
      }}>
        <div style={{
          fontSize: 14,
          fontWeight: 500,
          color: '#262626',
          marginBottom: 12
        }}>
          Add Section
        </div>
        <Space direction="vertical" style={{ width: '100%' }} size={8}>
          <Button
            block
            size="large"
            icon={<PlusOutlined />}
            onClick={handleAddGridSection}
            type="dashed"
            style={{ 
              height: 44,
              fontSize: 14,
              fontWeight: 500,
              borderColor: '#1890ff',
              color: '#1890ff',
              borderRadius: 6,
              transition: 'all 0.2s ease'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#f0f9ff';
              e.currentTarget.style.borderColor = '#69c0ff';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'white';
              e.currentTarget.style.borderColor = '#1890ff';
            }}
          >
            Grid Section
          </Button>
          <Button
            block
            size="large"
            icon={<PlusOutlined />}
            onClick={handleAddStackSection}
            type="dashed"
            style={{ 
              height: 44,
              fontSize: 14,
              fontWeight: 500,
              borderColor: '#1890ff',
              color: '#1890ff',
              borderRadius: 6,
              transition: 'all 0.2s ease'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#f0f9ff';
              e.currentTarget.style.borderColor = '#69c0ff';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'white';
              e.currentTarget.style.borderColor = '#1890ff';
            }}
          >
            Stack Section
          </Button>
        </Space>
        {schema.sections.length > 0 && (
          <div style={{
            fontSize: 12,
            color: '#8c8c8c',
            marginTop: 12,
            textAlign: 'center',
            padding: '8px',
            background: '#f6f8fa',
            borderRadius: 4
          }}>
            {schema.sections.length} section{schema.sections.length > 1 ? 's' : ''} added
          </div>
        )}
      </div>

      {/* Widget Categories */}
      <div style={{ margin: '0 16px 16px 16px' }}>
        {WIDGET_CATEGORIES.map(category => (
          <div key={category.key} style={{
            marginBottom: 20,
            background: 'white',
            borderRadius: 8,
            border: '1px solid #e8e8e8',
            overflow: 'hidden'
          }}>
            <div style={{
              padding: '12px 16px',
              borderBottom: '1px solid #f0f0f0',
              background: '#fafafa'
            }}>
              <div style={{
                display: 'flex',
                alignItems: 'center'
              }}>
                <div style={{
                  width: 3,
                  height: 16,
                  background: '#1890ff',
                  borderRadius: 2,
                  marginRight: 8
                }} />
                <span style={{
                  fontSize: 14,
                  fontWeight: 600,
                  color: '#262626'
                }}>
                  {category.label}
                </span>
                <span style={{
                  fontSize: 12,
                  color: '#8c8c8c',
                  marginLeft: 8
                }}>
                  ({category.widgets.length})
                </span>
              </div>
            </div>
            <div style={{ padding: '12px 16px' }}>
              <Row gutter={[8, 8]}>
                {category.widgets.map(widget => (
                  <Col key={widget.type} span={12}>
                    <DraggableWidget widget={widget} />
                  </Col>
                ))}
              </Row>
            </div>
          </div>
        ))}
      </div>

      <Divider />
      
      {/* Usage Instructions */}
      <div style={{ 
        margin: '0 16px 16px 16px',
        padding: '16px',
        background: 'linear-gradient(135deg, #f6ffed 0%, #f0f9ff 100%)',
        borderRadius: 8,
        border: '1px solid #b7eb8f'
      }}>
        <div style={{
          fontSize: 13,
          fontWeight: 500,
          color: '#389e0d',
          marginBottom: 8,
          display: 'flex',
          alignItems: 'center'
        }}>
          <span style={{
            display: 'inline-block',
            width: 6,
            height: 6,
            background: '#52c41a',
            borderRadius: '50%',
            marginRight: 6
          }} />
          How to use
        </div>
        <div style={{ fontSize: 12, color: '#52c41a', lineHeight: 1.5 }}>
          1. Add Grid/Stack sections first<br/>
          2. Drag widgets into sections<br/>
          3. Configure properties in right panel<br/>
          4. Preview and save your form
        </div>
      </div>
    </div>
  );
};

export default WidgetPalette;
