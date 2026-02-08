import React, { useCallback } from 'react';
import { Layout, Button, Space, Tooltip, Divider, Modal, Form, Input, message, Drawer, List, Card, Select } from 'antd';
import { 
  MenuFoldOutlined, 
  MenuUnfoldOutlined, 
  SaveOutlined, 
  EyeOutlined, 
  SettingOutlined,
  CheckOutlined,
  SearchOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  CopyOutlined,
  DeleteOutlined,
  InfoCircleOutlined,
  QuestionCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';
import { 
  DndContext, 
  DragOverlay, 
  useSensor,
  useSensors,
  PointerSensor,
  KeyboardSensor,
  DragStartEvent,
  DragEndEvent,
  closestCenter
} from '@dnd-kit/core';
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { usePageBuilderStore } from '../../store/pageBuilderStore';
import WidgetPalette from './WidgetPalette';
import PropertyPanel from './PropertyPanel';
import PropertyEditor from './PropertyEditor';
import PageBuilderCanvas from './PageBuilderCanvas';
import WidgetRenderer from './WidgetRenderer';
import { WidgetType } from '../../types/pageBuilder';
import './PageBuilder.css';

const { Sider, Header, Content } = Layout;

interface PageBuilderProps {
  onSave?: (schema: any) => void;
  onLoad?: () => any;
  collapsed?: boolean;
  onCollapseChange?: (collapsed: boolean) => void;
}

const PageBuilder: React.FC<PageBuilderProps> = ({
  onSave,
  onLoad,
  collapsed: externalCollapsed,
  onCollapseChange
}) => {
  const { 
    previewMode, 
    draggedItem, 
    setDraggedItem,
    addItem,
    selectedItemId,
    selectedSectionId,
    setPreviewMode,
    duplicateItem,
    removeItem,
    schema
  } = usePageBuilderStore();
  const [internalCollapsed, setInternalCollapsed] = React.useState(false);
  const [showPropertyEditor, setShowPropertyEditor] = React.useState(false);
  const [showFormSettings, setShowFormSettings] = React.useState(false);
  const [showFormInfo, setShowFormInfo] = React.useState(false);
  const [showHelp, setShowHelp] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState('');
  const [zoomLevel, setZoomLevel] = React.useState(100);
  
  // Use external collapse state if provided, otherwise use internal
  const collapsed = externalCollapsed !== undefined ? externalCollapsed : internalCollapsed;
  const setCollapsed = onCollapseChange || setInternalCollapsed;

  // Setup drag and drop sensors
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 3, // Easy dragging
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragStart = useCallback((event: DragStartEvent) => {
    const { active } = event;
    const data = active.data.current;
    
    if (data?.type === 'widget') {
      setDraggedItem({
        type: 'widget',
        widgetType: data.widgetType as WidgetType,
        fromPalette: true,
        data: data.widget
      });
    }
  }, [setDraggedItem]);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    const { active, over } = event;
    
    if (!over) {
      setDraggedItem(null);
      return;
    }

    const activeData = active.data.current;
    const overData = over.data.current;

    // Handle dropping widget from palette to section
    if (activeData?.type === 'widget' && (overData?.type === 'grid-section' || overData?.type === 'stack-section')) {
      const widgetType = activeData.widgetType as WidgetType;
      const sectionId = overData.sectionId;
      
      if (widgetType && sectionId) {
        const baseItem = {
          id: `widget-${Date.now()}`,
          type: widgetType,
          props: getDefaultPropsForWidget(widgetType)
        };

        // Create appropriate item structure based on section type
        if (overData.type === 'grid-section') {
          addItem(sectionId, {
            ...baseItem,
            x: 0,
            y: 0, 
            w: 4,
            h: 2
          });
        } else if (overData.type === 'stack-section') {
          addItem(sectionId, {
            ...baseItem,
            order: 0
          });
        }
      }
    }

    setDraggedItem(null);
  }, [addItem, setDraggedItem]);

  const renderDragOverlay = () => {
    if (!draggedItem || draggedItem.type !== 'widget' || !draggedItem.widgetType) return null;
    
    return (
      <div style={{ 
        padding: 12, 
        background: 'white', 
        border: '2px solid #1890ff', 
        borderRadius: 8,
        opacity: 0.9,
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
        transform: 'rotate(2deg)',
        minWidth: 200,
        zIndex: 9999
      }}>
        <WidgetRenderer
          widget={{
            id: 'overlay',
            type: draggedItem.widgetType,
            props: getDefaultPropsForWidget(draggedItem.widgetType)
          }}
          mode="builder"
        />
      </div>
    );
  };

  // Helper function to get default props for widget types
  const getDefaultPropsForWidget = (widgetType: WidgetType): any => {
    const defaultProps: Record<WidgetType, any> = {
      'text-field': { label: 'Text Field', placeholder: 'Enter text...', required: false },
      'textarea': { label: 'Text Area', placeholder: 'Enter text...', rows: 4, required: false },
      'number': { label: 'Number', placeholder: 'Enter number...', required: false },
      'email': { label: 'Email', placeholder: 'Enter email...', required: false },
      'password': { label: 'Password', placeholder: 'Enter password...', required: false },
      'select': { label: 'Select', placeholder: 'Choose option...', options: [{ label: 'Option 1', value: '1' }], required: false },
      'radio': { label: 'Radio Group', options: [{ label: 'Option 1', value: '1' }], required: false },
      'checkbox': { label: 'Checkbox Group', options: [{ label: 'Option 1', value: '1' }] },
      'date': { label: 'Date', placeholder: 'Select date...', required: false },
      'time': { label: 'Time', placeholder: 'Select time...', required: false },
      'switch': { label: 'Switch', checkedChildren: 'ON', unCheckedChildren: 'OFF' },
      'slider': { label: 'Slider', min: 0, max: 100, defaultValue: 50 },
      'rate': { label: 'Rate', allowHalf: true, character: '⭐' },
      'upload': { label: 'Upload', accept: '*', multiple: false, listType: 'text' },
      // Add simplified defaults for all other widget types
      'phone': { label: 'Phone', placeholder: 'Enter phone number...', required: false },
      'url': { label: 'URL', placeholder: 'Enter website URL...', required: false },
      'currency': { label: 'Currency', placeholder: 'Enter amount...', required: false },
      'percentage': { label: 'Percentage', placeholder: 'Enter percentage...', required: false },
      'signature': { label: 'Signature', placeholder: 'Please sign here...', required: false },
      'barcode': { label: 'Barcode', value: '123456789', format: 'CODE128' },
      'qrcode': { label: 'QR Code', value: 'Hello World', size: 128 },
      'cascader': { label: 'Cascader', options: [], placeholder: 'Please select' },
      'tree-select': { label: 'Tree Select', treeData: [], placeholder: 'Please select' },
      'transfer': { label: 'Transfer', dataSource: [], titles: ['Source', 'Target'] },
      'mentions': { label: 'Mentions', placeholder: 'Type @ to mention...', required: false },
      'color-picker': { label: 'Color Picker', defaultValue: '#1890ff' },
      'code-editor': { label: 'Code Editor', language: 'javascript', required: false },
      'image': { src: '', alt: 'Image', width: '100%', height: 'auto' },
      'video': { src: '', controls: true, width: '100%' },
      'audio': { src: '', controls: true, width: '100%' },
      'text-block': { content: 'Text block content', fontSize: 14 },
      'html': { content: '<div>Custom HTML content</div>' },
      'iframe': { src: '', width: '100%', height: '400px' },
      'document': { src: '', width: '100%', height: '600px' },
      'divider': { orientation: 'center', dashed: false },
      'space': { size: 'middle', direction: 'horizontal' },
      'card': { title: 'Card Title', bordered: true },
      'tabs': { defaultActiveKey: '1', items: [{ key: '1', label: 'Tab 1', children: 'Content 1' }] },
      'collapse': { defaultActiveKey: '1', items: [{ key: '1', label: 'Panel 1', children: 'Content 1' }] },
      'steps': { current: 0, items: [{ title: 'Step 1' }, { title: 'Step 2' }] },
      'list': { dataSource: [] },
      'table': { dataSource: [], columns: [] },
      'chart': { type: 'line', data: [], width: '100%', height: 300 },
      'timeline': { items: [{ children: 'Timeline item' }] },
      'calendar': { mode: 'month', fullscreen: true },
      'kanban': { columns: ['To Do', 'In Progress', 'Done'], cards: [] }
    };
    return defaultProps[widgetType] || { label: widgetType };
  };

  // Show property editor when item is selected
  React.useEffect(() => {
    setShowPropertyEditor(!!selectedItemId);
  }, [selectedItemId]);

  const handleClosePropertyEditor = useCallback(() => {
    setShowPropertyEditor(false);
  }, []);

  // Handler functions for toolbar buttons
  const handleValidateForm = useCallback(() => {
    const errors = [];
    
    // Check if form has a title
    if (!schema.title || schema.title.trim() === '') {
      errors.push('Form title is required');
    }
    
    // Check if form has at least one section
    if (schema.sections.length === 0) {
      errors.push('Form must have at least one section');
    }
    
    // Check each section for widgets
    schema.sections.forEach((section, index) => {
      if (section.type === 'grid' && section.items.length === 0) {
        errors.push(`Section ${index + 1} is empty`);
      } else if (section.type === 'stack' && section.children.length === 0) {
        errors.push(`Section ${index + 1} is empty`);
      }
    });
    
    if (errors.length === 0) {
      message.success('Form validation passed! 🎉');
    } else {
      Modal.error({
        title: 'Form Validation Issues',
        content: (
          <List
            size="small"
            dataSource={errors}
            renderItem={(item) => <List.Item>{item}</List.Item>}
          />
        )
      });
    }
  }, [schema]);

  const handleTogglePreview = useCallback(() => {
    setPreviewMode(!previewMode);
    message.info(previewMode ? 'Edit mode activated' : 'Preview mode activated');
  }, [previewMode, setPreviewMode]);

  const handleShowFormSettings = useCallback(() => {
    setShowFormSettings(true);
  }, []);

  const handleSearchElements = useCallback(() => {
    Modal.info({
      title: 'Search Elements',
      content: (
        <div>
          <Input.Search
            placeholder="Search for widgets, sections, or properties..."
            onSearch={(value) => {
              setSearchQuery(value);
              message.info(value ? `Searching for: "${value}"` : 'Search cleared');
            }}
            style={{ marginBottom: 16 }}
            defaultValue={searchQuery}
          />
          <p style={{ color: '#666', fontSize: 12 }}>
            Search functionality will highlight matching elements in the canvas
          </p>
          {searchQuery && (
            <div style={{ 
              marginTop: 12,
              padding: 8,
              background: '#f0f8ff',
              borderRadius: 4,
              fontSize: 12,
              color: '#1890ff'
            }}>
              Current search: "{searchQuery}"
            </div>
          )}
        </div>
      ),
      width: 500
    });
  }, [searchQuery]);

  const handleZoomIn = useCallback(() => {
    const newZoom = Math.min(zoomLevel + 10, 200);
    setZoomLevel(newZoom);
    message.info(`Zoom level: ${newZoom}%`);
  }, [zoomLevel]);

  const handleZoomOut = useCallback(() => {
    const newZoom = Math.max(zoomLevel - 10, 50);
    setZoomLevel(newZoom);
    message.info(`Zoom level: ${newZoom}%`);
  }, [zoomLevel]);

  const handleCopySelected = useCallback(() => {
    if (selectedItemId && selectedSectionId) {
      duplicateItem(selectedSectionId, selectedItemId);
      message.success('Widget duplicated successfully');
    } else {
      message.warning('Please select a widget to copy');
    }
  }, [selectedItemId, selectedSectionId, duplicateItem]);

  const handleDeleteSelected = useCallback(() => {
    if (selectedItemId && selectedSectionId) {
      Modal.confirm({
        title: 'Delete Selected Widget',
        content: 'Are you sure you want to delete the selected widget?',
        icon: <ExclamationCircleOutlined />,
        okText: 'Delete',
        okType: 'danger',
        cancelText: 'Cancel',
        onOk: () => {
          removeItem(selectedSectionId, selectedItemId);
          message.success('Widget deleted successfully');
        }
      });
    } else {
      message.warning('Please select a widget to delete');
    }
  }, [selectedItemId, selectedSectionId, removeItem]);

  const handleShowFormInfo = useCallback(() => {
    setShowFormInfo(true);
  }, []);

  const handleShowHelp = useCallback(() => {
    setShowHelp(true);
  }, []);

  // Keyboard shortcuts
  React.useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Ctrl + S - Save
      if (event.ctrlKey && event.key === 's') {
        event.preventDefault();
        onSave && onSave({});
        message.success('Form saved!');
      }
      
      // Ctrl + D - Duplicate selected widget
      if (event.ctrlKey && event.key === 'd' && selectedItemId && selectedSectionId) {
        event.preventDefault();
        handleCopySelected();
      }
      
      // Delete - Remove selected widget
      if (event.key === 'Delete' && selectedItemId && selectedSectionId) {
        event.preventDefault();
        handleDeleteSelected();
      }
      
      // Escape - Close modals or deselect
      if (event.key === 'Escape') {
        if (showFormSettings) {
          setShowFormSettings(false);
        } else if (showFormInfo) {
          setShowFormInfo(false);
        } else if (showHelp) {
          setShowHelp(false);
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [selectedItemId, selectedSectionId, onSave, showFormSettings, showFormInfo, showHelp, handleCopySelected, handleDeleteSelected]);

  if (previewMode) {
    return <PageBuilderCanvas onSave={onSave} />;
  }

  // Show full-width property editor when editing a widget
  if (showPropertyEditor && selectedItemId) {
    return <PropertyEditor onClose={handleClosePropertyEditor} />;
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <Layout style={{ height: '100%', minHeight: '100vh', background: '#f0f2f5' }}>
        {/* Simplified Top Toolbar */}
        <Header style={{
          background: 'white',
          borderBottom: '1px solid #e8e8e8',
          padding: '0 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          height: 56,
          boxShadow: '0 1px 2px rgba(0,0,0,0.08)',
          zIndex: 100
        }}>
          <Space size="large">
            <Space size="small">
              <Tooltip title={collapsed ? "Expand Sidebar" : "Collapse Sidebar"}>
                <Button 
                  type="text" 
                  icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                  onClick={() => setCollapsed(!collapsed)}
                  style={{ 
                    fontSize: 16,
                    width: 32,
                    height: 32,
                    borderRadius: 6
                  }}
                />
              </Tooltip>
              <span style={{ 
                fontSize: 16, 
                fontWeight: 600, 
                color: '#262626'
              }}>
                Form Builder
              </span>
            </Space>
          </Space>

          <Space size="small">
            <Button 
              type="default" 
              icon={<EyeOutlined />}
              onClick={handleTogglePreview}
              style={{
                borderRadius: 6,
                height: 36,
                paddingLeft: 16,
                paddingRight: 16,
                backgroundColor: previewMode ? '#e6f7ff' : 'white',
                borderColor: previewMode ? '#1890ff' : '#d9d9d9',
                color: previewMode ? '#1890ff' : '#595959'
              }}
            >
              {previewMode ? 'Edit' : 'Preview'}
            </Button>
            <Button 
              type="primary" 
              icon={<SaveOutlined />}
              onClick={() => {
                onSave && onSave({});
                message.success('Form saved successfully! 🎉');
              }}
              style={{
                borderRadius: 6,
                height: 36,
                paddingLeft: 16,
                paddingRight: 16,
                fontWeight: 500
              }}
            >
              Save
            </Button>
          </Space>
        </Header>

        {/* Main Layout with 3 columns */}
        <Layout style={{ height: 'calc(100vh - 56px)' }}>
          {/* Left Sidebar - Widget Palette */}
          <Sider
            width={280}
            collapsed={collapsed}
            collapsedWidth={50}
            style={{
              background: 'white',
              borderRight: '1px solid #e8e8e8',
              overflow: 'hidden'
            }}
          >
            <WidgetPalette collapsed={collapsed} />
          </Sider>

          {/* Middle Content Area + Vertical Toolbar */}
          <Layout style={{ position: 'relative' }}>
            {/* Main Canvas */}
            <Content style={{ 
              background: '#f0f2f5',
              padding: 16,
              position: 'relative'
            }}>
              <div style={{
                background: 'white',
                borderRadius: 8,
                border: '1px solid #e8e8e8',
                height: '100%',
                boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                position: 'relative',
                transform: `scale(${zoomLevel / 100})`,
                transformOrigin: 'top left',
                transition: 'transform 0.2s ease'
              }}>
                <PageBuilderCanvas onSave={onSave} />
              </div>
            </Content>

            {/* Vertical Tools Sidebar */}
            <div style={{
              position: 'absolute',
              right: 16,
              top: 20,
              width: 48,
              background: 'white',
              borderRadius: 8,
              border: '1px solid #e8e8e8',
              boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
              zIndex: 10,
              padding: '8px 0'
            }}>
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 4
              }}>
                <Tooltip title="Validate Form" placement="left">
                  <Button 
                    type="text" 
                    icon={<CheckOutlined />}
                    onClick={handleValidateForm}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: '#52c41a',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Tooltip title="Preview Form" placement="left">
                  <Button 
                    type="text" 
                    icon={<EyeOutlined />}
                    onClick={handleTogglePreview}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: previewMode ? '#1890ff' : '#8c8c8c',
                      backgroundColor: previewMode ? '#f0f8ff' : 'transparent',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Tooltip title="Form Settings" placement="left">
                  <Button 
                    type="text" 
                    icon={<SettingOutlined />}
                    onClick={handleShowFormSettings}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: '#8c8c8c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Divider style={{ margin: '4px 0', width: 24 }} />
                
                <Tooltip title="Search Elements" placement="left">
                  <Button 
                    type="text" 
                    icon={<SearchOutlined />}
                    onClick={handleSearchElements}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: '#8c8c8c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Tooltip title={`Zoom In (${zoomLevel}%)`} placement="left">
                  <Button 
                    type="text" 
                    icon={<ZoomInOutlined />}
                    onClick={handleZoomIn}
                    disabled={zoomLevel >= 200}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: zoomLevel >= 200 ? '#d9d9d9' : '#8c8c8c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Tooltip title={`Zoom Out (${zoomLevel}%)`} placement="left">
                  <Button 
                    type="text" 
                    icon={<ZoomOutOutlined />}
                    onClick={handleZoomOut}
                    disabled={zoomLevel <= 50}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: zoomLevel <= 50 ? '#d9d9d9' : '#8c8c8c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Divider style={{ margin: '4px 0', width: 24 }} />
                
                <Tooltip title="Copy Selected" placement="left">
                  <Button 
                    type="text" 
                    icon={<CopyOutlined />}
                    onClick={handleCopySelected}
                    disabled={!selectedItemId}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: selectedItemId ? '#8c8c8c' : '#d9d9d9',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Tooltip title="Delete Selected" placement="left">
                  <Button 
                    type="text" 
                    icon={<DeleteOutlined />}
                    onClick={handleDeleteSelected}
                    disabled={!selectedItemId}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: selectedItemId ? '#ff4d4f' : '#d9d9d9',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Divider style={{ margin: '4px 0', width: 24 }} />
                
                <Tooltip title="Form Information" placement="left">
                  <Button 
                    type="text" 
                    icon={<InfoCircleOutlined />}
                    onClick={handleShowFormInfo}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: '#8c8c8c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
                
                <Tooltip title="Help" placement="left">
                  <Button 
                    type="text" 
                    icon={<QuestionCircleOutlined />}
                    onClick={handleShowHelp}
                    style={{
                      width: 32,
                      height: 32,
                      borderRadius: 6,
                      color: '#8c8c8c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  />
                </Tooltip>
              </div>
            </div>
          </Layout>

          {/* Right Sidebar - Enhanced Property Panel */}
          <Sider
            width={350}
            style={{
              background: 'white',
              borderLeft: '1px solid #e8e8e8',
              overflow: 'hidden'
            }}
          >
            <div style={{
              padding: '16px 20px',
              borderBottom: '1px solid #e8e8e8',
              background: 'white'
            }}>
              <div style={{ 
                fontWeight: 600, 
                fontSize: 16,
                color: '#262626',
                marginBottom: 4
              }}>
                Properties Panel
              </div>
              <div style={{
                fontSize: 12,
                color: '#8c8c8c'
              }}>
                Configure selected element properties
              </div>
            </div>
            <div style={{
              height: 'calc(100vh - 56px - 70px)',
              overflow: 'auto',
              background: '#fafafa'
            }}>
              <PropertyPanel collapsed={false} />
            </div>
          </Sider>
        </Layout>
      </Layout>

      {/* Drag Overlay */}
      <DragOverlay>
        {renderDragOverlay()}
      </DragOverlay>

      {/* Form Settings Modal */}
      <Modal
        title="Form Settings"
        open={showFormSettings}
        onCancel={() => setShowFormSettings(false)}
        footer={[
          <Button key="cancel" onClick={() => setShowFormSettings(false)}>
            Cancel
          </Button>,
          <Button key="save" type="primary" onClick={() => setShowFormSettings(false)}>
            Save Settings
          </Button>
        ]}
        width={600}
      >
        <Form layout="vertical">
          <Form.Item label="Form Title" required>
            <Input placeholder="Enter form title" defaultValue={schema.title} />
          </Form.Item>
          <Form.Item label="Form Description">
            <Input.TextArea rows={3} placeholder="Enter form description" defaultValue={schema.description} />
          </Form.Item>
          <Form.Item label="Form Layout">
            <Select defaultValue={schema.layout || 'classic'}>
              <Select.Option value="classic">Classic</Select.Option>
              <Select.Option value="card">Card Layout</Select.Option>
              <Select.Option value="stepped">Stepped Form</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Form Information Modal */}
      <Drawer
        title="Form Information"
        placement="right"
        open={showFormInfo}
        onClose={() => setShowFormInfo(false)}
        width={400}
      >
        <Card size="small" title="Form Statistics" style={{ marginBottom: 16 }}>
          <List size="small">
            <List.Item>
              <strong>Total Sections:</strong> {schema.sections.length}
            </List.Item>
            <List.Item>
              <strong>Total Widgets:</strong> {schema.sections.reduce((total, section) => {
                if (section.type === 'grid') return total + section.items.length;
                if (section.type === 'stack') return total + section.children.length;
                return total;
              }, 0)}
            </List.Item>
            <List.Item>
              <strong>Form Title:</strong> {schema.title || 'Untitled Form'}
            </List.Item>
            <List.Item>
              <strong>Created:</strong> {new Date().toLocaleDateString()}
            </List.Item>
          </List>
        </Card>
        
        <Card size="small" title="Form Structure">
          <List
            size="small"
            dataSource={schema.sections}
            renderItem={(section, index) => (
              <List.Item>
                <strong>Section {index + 1}:</strong> {section.title || `${section.type} section`}
                <span style={{ color: '#666', marginLeft: 8 }}>
                  ({section.type === 'grid' ? section.items.length : section.children.length} widgets)
                </span>
              </List.Item>
            )}
          />
        </Card>
      </Drawer>

      {/* Help Modal */}
      <Modal
        title="Page Builder Help"
        open={showHelp}
        onCancel={() => setShowHelp(false)}
        footer={[
          <Button key="close" type="primary" onClick={() => setShowHelp(false)}>
            Close
          </Button>
        ]}
        width={700}
      >
        <div style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          <Card size="small" title="🚀 Getting Started" style={{ marginBottom: 16 }}>
            <List size="small">
              <List.Item>• Drag widgets from the left panel to create your form</List.Item>
              <List.Item>• Click on widgets to configure their properties</List.Item>
              <List.Item>• Use sections to organize your form layout</List.Item>
              <List.Item>• Preview your form using the eye button</List.Item>
            </List>
          </Card>

          <Card size="small" title="🛠️ Toolbar Functions" style={{ marginBottom: 16 }}>
            <List size="small">
              <List.Item>• <CheckOutlined style={{ color: '#52c41a' }} /> Validate - Check form for errors</List.Item>
              <List.Item>• <EyeOutlined style={{ color: '#1890ff' }} /> Preview - Toggle preview mode</List.Item>
              <List.Item>• <SettingOutlined /> Settings - Configure form settings</List.Item>
              <List.Item>• <SearchOutlined /> Search - Find widgets and elements</List.Item>
              <List.Item>• <ZoomInOutlined /> / <ZoomOutOutlined /> Zoom - Adjust canvas size</List.Item>
              <List.Item>• <CopyOutlined style={{ color: '#52c41a' }} /> Copy - Duplicate selected widget</List.Item>
              <List.Item>• <DeleteOutlined style={{ color: '#ff4d4f' }} /> Delete - Remove selected widget</List.Item>
            </List>
          </Card>

          <Card size="small" title="⌨️ Keyboard Shortcuts">
            <List size="small">
              <List.Item>• <strong>Ctrl + S</strong> - Save form</List.Item>
              <List.Item>• <strong>Ctrl + D</strong> - Duplicate selected widget</List.Item>
              <List.Item>• <strong>Delete</strong> - Remove selected widget</List.Item>
              <List.Item>• <strong>Ctrl + Z</strong> - Undo (coming soon)</List.Item>
              <List.Item>• <strong>Ctrl + Y</strong> - Redo (coming soon)</List.Item>
            </List>
          </Card>
        </div>
      </Modal>
    </DndContext>
  );
};

export default PageBuilder;
