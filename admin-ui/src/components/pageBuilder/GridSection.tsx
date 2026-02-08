import React, { useCallback, useMemo, useState } from 'react';
import { Card, Typography, Button, Space, Popconfirm, Tooltip, Empty, Modal, Input, Tabs, Row, Col, message } from 'antd';
import { useDroppable } from '@dnd-kit/core';
import RGL, { WidthProvider, Layout } from 'react-grid-layout';
import { 
  DeleteOutlined, 
  CopyOutlined, 
  DragOutlined,
  SettingOutlined,
  PlusOutlined,
  SearchOutlined,
  AppstoreOutlined
} from '@ant-design/icons';
import { GridSection as GridSectionType, GridItem, WidgetType } from '../../types/pageBuilder';
import { usePageBuilderStore } from '../../store/pageBuilderStore';
import WidgetRenderer from './WidgetRenderer';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';

const { Title, Text } = Typography;
const ReactGridLayout = WidthProvider(RGL);

// Widget categories and configurations
const WIDGET_CATEGORIES = {
  form: {
    title: 'Form Fields',
    widgets: [
      { type: 'text-field', label: 'Text Field', icon: '📝' },
      { type: 'textarea', label: 'Text Area', icon: '📄' },
      { type: 'number', label: 'Number', icon: '🔢' },
      { type: 'email', label: 'Email', icon: '📧' },
      { type: 'password', label: 'Password', icon: '🔒' },
      { type: 'select', label: 'Select', icon: '📋' },
      { type: 'radio', label: 'Radio Group', icon: '🔘' },
      { type: 'checkbox', label: 'Checkbox', icon: '☑️' },
      { type: 'date', label: 'Date', icon: '📅' },
      { type: 'time', label: 'Time', icon: '⏰' },
      { type: 'switch', label: 'Switch', icon: '🔄' },
      { type: 'slider', label: 'Slider', icon: '🎚️' },
      { type: 'rate', label: 'Rating', icon: '⭐' },
      { type: 'upload', label: 'Upload', icon: '📎' }
    ]
  },
  layout: {
    title: 'Layout',
    widgets: [
      { type: 'divider', label: 'Divider', icon: '➖' },
      { type: 'space', label: 'Space', icon: '⬜' },
      { type: 'card', label: 'Card', icon: '🃏' },
      { type: 'tabs', label: 'Tabs', icon: '📑' },
      { type: 'collapse', label: 'Collapse', icon: '🗂️' },
      { type: 'steps', label: 'Steps', icon: '👣' }
    ]
  },
  display: {
    title: 'Display',
    widgets: [
      { type: 'text-block', label: 'Text Block', icon: '📝' },
      { type: 'image', label: 'Image', icon: '🖼️' },
      { type: 'video', label: 'Video', icon: '🎥' },
      { type: 'html', label: 'HTML', icon: '🌐' },
      { type: 'chart', label: 'Chart', icon: '📊' },
      { type: 'table', label: 'Table', icon: '📊' },
      { type: 'list', label: 'List', icon: '📝' }
    ]
  }
};

interface GridSectionProps {
  section: GridSectionType;
  isSelected?: boolean;
  previewMode?: boolean;
}

const GridSection: React.FC<GridSectionProps> = ({ 
  section, 
  isSelected = false, 
  previewMode = false 
}) => {
  const {
    removeSection,
    updateItem,
    removeItem,
    duplicateItem,
    selectItem,
    selectedItemId,
    selectedSectionId,
    addItem
  } = usePageBuilderStore();

  const [showWidgetModal, setShowWidgetModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('form');

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

  const handleAddWidget = (widgetType: WidgetType) => {
    const newItem: GridItem = {
      id: `widget-${Date.now()}`,
      type: widgetType,
      x: 0,
      y: 0,
      w: 4,
      h: 2,
      props: getDefaultPropsForWidget(widgetType)
    };
    
    addItem(section.id, newItem);
    setShowWidgetModal(false);
    setSearchTerm('');
    setSelectedCategory('form');
    
    // Show success message
    message.success(`${widgetType.replace('-', ' ')} widget added successfully!`);
  };

  // Setup drop zone for dragging widgets into grid
  const { setNodeRef, isOver } = useDroppable({
    id: `grid-section-${section.id}`,
    data: {
      type: 'grid-section',
      sectionId: section.id,
      accepts: ['widget']
    }
  });

  // Convert GridItems to react-grid-layout format
  const layouts = useMemo(() => {
    const layout: Layout[] = section.items.map(item => ({
      i: item.id,
      x: item.x,
      y: item.y,
      w: item.w,
      h: item.h,
      minW: item.minW,
      minH: item.minH,
      maxW: item.maxW,
      maxH: item.maxH,
      static: item.static || false,
      isDraggable: !previewMode && (item.isDraggable !== false),
      isResizable: !previewMode && (item.isResizable !== false)
    }));
    return layout;
  }, [section.items, previewMode]);

  // Handle layout changes (drag/resize)
  const handleLayoutChange = useCallback((newLayout: Layout[]) => {
    if (previewMode) return;
    
    newLayout.forEach(layoutItem => {
      const gridItem = section.items.find(item => item.id === layoutItem.i);
      if (gridItem) {
        const updates: Partial<GridItem> = {
          x: layoutItem.x,
          y: layoutItem.y,
          w: layoutItem.w,
          h: layoutItem.h
        };
        updateItem(section.id, layoutItem.i, updates);
      }
    });
  }, [section.id, section.items, updateItem, previewMode]);

  const handleItemClick = useCallback((itemId: string, e: React.MouseEvent) => {
    if (previewMode) return;
    e.stopPropagation();
    try {
      console.log('Selecting item:', itemId, 'in section:', section.id);
      selectItem(itemId, section.id);
    } catch (error) {
      console.error('Error selecting item:', error);
      message.error('Failed to select widget');
    }
  }, [selectItem, section.id, previewMode]);

  const handleSectionClick = useCallback((e: React.MouseEvent) => {
    if (previewMode) return;
    e.stopPropagation();
    try {
      console.log('Selecting section:', section.id);
      selectItem(undefined, section.id);
    } catch (error) {
      console.error('Error selecting section:', error);
      message.error('Failed to select section');
    }
  }, [selectItem, section.id, previewMode]);

  const handleRemoveSection = useCallback(() => {
    try {
      removeSection(section.id);
      message.success('Section deleted successfully');
    } catch (error) {
      message.error('Failed to delete section');
      console.error('Error deleting section:', error);
    }
  }, [removeSection, section.id]);

  const handleEditSection = useCallback(() => {
    try {
      selectItem(undefined, section.id);
      message.info('Section selected for editing');
    } catch (error) {
      message.error('Failed to select section');
      console.error('Error selecting section:', error);
    }
  }, [selectItem, section.id]);

  const handleRemoveItem = useCallback((itemId: string, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
    }
    try {
      removeItem(section.id, itemId);
      message.success('Widget deleted successfully');
    } catch (error) {
      message.error('Failed to delete widget');
      console.error('Error deleting widget:', error);
    }
  }, [removeItem, section.id]);

  const handleDuplicateItem = useCallback((itemId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      duplicateItem(section.id, itemId);
      message.success('Widget duplicated successfully');
    } catch (error) {
      message.error('Failed to duplicate widget');
      console.error('Error duplicating widget:', error);
    }
  }, [duplicateItem, section.id]);

  const handleEditWidget = useCallback((itemId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      selectItem(itemId, section.id);
      message.info('Widget selected for editing');
    } catch (error) {
      message.error('Failed to select widget');
      console.error('Error selecting widget:', error);
    }
  }, [selectItem, section.id]);

  // Keyboard shortcuts for selected widget
  React.useEffect(() => {
    if (!isSelected || previewMode) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (selectedItemId && selectedSectionId === section.id) {
        // Delete key - remove selected widget
        if (event.key === 'Delete' || event.key === 'Backspace') {
          event.preventDefault();
          handleRemoveItem(selectedItemId);
        }
        // Ctrl+D - duplicate widget  
        if (event.ctrlKey && event.key === 'd') {
          event.preventDefault();
          handleDuplicateItem(selectedItemId, { preventDefault: () => {}, stopPropagation: () => {} } as any);
        }
        // Enter or Space - edit widget
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          handleEditWidget(selectedItemId, { preventDefault: () => {}, stopPropagation: () => {} } as any);
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isSelected, previewMode, selectedItemId, selectedSectionId, section.id, handleRemoveItem, handleDuplicateItem, handleEditWidget]);

  const renderGridItem = useCallback((item: GridItem) => {
    const isItemSelected = selectedItemId === item.id && selectedSectionId === section.id;
    
    return (
      <div
        key={item.id}
        onClick={(e) => handleItemClick(item.id, e)}
        style={{
          position: 'relative',
          height: '100%',
          border: isItemSelected ? '2px solid #1890ff' : '1px solid transparent',
          borderRadius: 6,
          overflow: 'hidden',
          cursor: previewMode ? 'default' : 'pointer',
          transition: 'all 0.2s ease',
          background: isItemSelected ? '#f0f8ff' : 'white'
        }}
        onMouseEnter={(e) => {
          if (!previewMode && !isItemSelected) {
            e.currentTarget.style.boxShadow = '0 4px 12px rgba(24, 144, 255, 0.15)';
            e.currentTarget.style.borderColor = '#91d5ff';
          }
        }}
        onMouseLeave={(e) => {
          if (!previewMode && !isItemSelected) {
            e.currentTarget.style.boxShadow = 'none';
            e.currentTarget.style.borderColor = 'transparent';
          }
        }}
      >
        {/* Widget Content */}
        <div style={{ height: '100%', padding: 4 }}>
          <WidgetRenderer 
            widget={{
              id: item.id,
              type: item.type,
              props: item.props
            }}
            mode={previewMode ? 'preview' : 'builder'}
          />
        </div>

        {/* Item Controls */}
        {!previewMode && isItemSelected && (
          <div 
            style={{
              position: 'absolute',
              top: -2,
              right: -2,
              background: 'white',
              border: '1px solid #1890ff',
              borderRadius: 6,
              padding: '6px',
              display: 'flex',
              gap: 4,
              boxShadow: '0 4px 12px rgba(24, 144, 255, 0.25)',
              zIndex: 10
            }}
          >
            <Tooltip title="Edit Properties" placement="top">
              <Button
                size="small"
                type="text"
                icon={<SettingOutlined style={{ fontSize: 16, color: '#1890ff' }} />}
                style={{
                  width: 28,
                  height: 28,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 4,
                  transition: 'all 0.2s ease',
                  background: '#f0f8ff'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#e6f7ff';
                  e.currentTarget.style.transform = 'scale(1.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = '#f0f8ff';
                  e.currentTarget.style.transform = 'scale(1)';
                }}
                onClick={(e) => handleEditWidget(item.id, e)}
              />
            </Tooltip>
            <Tooltip title="Duplicate" placement="top">
              <Button
                size="small"
                type="text"
                icon={<CopyOutlined style={{ fontSize: 16, color: '#52c41a' }} />}
                style={{
                  width: 28,
                  height: 28,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 4,
                  transition: 'all 0.2s ease'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#f6ffed';
                  e.currentTarget.style.transform = 'scale(1.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                  e.currentTarget.style.transform = 'scale(1)';
                }}
                onClick={(e) => handleDuplicateItem(item.id, e)}
              />
            </Tooltip>
            <Tooltip title="Delete" placement="top">
              <Popconfirm
                title="Delete this widget?"
                description="This action cannot be undone."
                onConfirm={() => handleRemoveItem(item.id)}
                okText="Delete"
                cancelText="Cancel"
                placement="topRight"
                okType="danger"
              >
                <Button
                  size="small"
                  type="text"
                  danger
                  icon={<DeleteOutlined style={{ fontSize: 16, color: '#ff4d4f' }} />}
                  style={{
                    width: 28,
                    height: 28,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    borderRadius: 4,
                    transition: 'all 0.2s ease'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = '#fff2f0';
                    e.currentTarget.style.transform = 'scale(1.1)';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.transform = 'scale(1)';
                  }}
                  onClick={(e) => e.stopPropagation()}
                />
              </Popconfirm>
            </Tooltip>
          </div>
        )}

        {/* Drag Handle */}
        {!previewMode && (
          <div
            className="react-grid-dragHandle"
            style={{
              position: 'absolute',
              top: 4,
              left: 4,
              background: 'rgba(0,0,0,0.1)',
              borderRadius: 2,
              padding: 2,
              cursor: 'move',
              display: isItemSelected ? 'block' : 'none'
            }}
          >
            <DragOutlined style={{ fontSize: 12, color: '#666' }} />
          </div>
        )}
      </div>
    );
  }, [selectedItemId, selectedSectionId, section.id, handleItemClick, handleDuplicateItem, handleRemoveItem, handleEditWidget, previewMode]);

  const sectionStyle: React.CSSProperties = {
    border: isSelected ? '2px solid #1890ff' : '1px solid #d9d9d9',
    borderRadius: 8,
    background: previewMode ? 'white' : (isOver ? '#f0f9ff' : '#fafafa'),
    minHeight: previewMode ? 'auto' : 200,
    position: 'relative',
    transition: 'all 0.2s ease'
  };

  if (previewMode) {
    return (
      <div style={sectionStyle}>
        {section.items.length > 0 ? (
          <ReactGridLayout
            className="layout"
            layout={layouts}
            cols={section.cols}
            rowHeight={section.rowHeight}
            onLayoutChange={handleLayoutChange}
            isDraggable={false}
            isResizable={false}
            margin={[8, 8]}
            containerPadding={[16, 16]}
            breakpoints={section.breakpoints}
          >
            {section.items.map(renderGridItem)}
          </ReactGridLayout>
        ) : null}
      </div>
    );
  }

  return (
    <Card
      style={sectionStyle}
      styles={{ body: { padding: 0 } }}
      onClick={handleSectionClick}
    >
      {/* Section Header */}
      <div 
        style={{ 
          padding: '12px 16px', 
          borderBottom: '1px solid #f0f0f0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: isSelected ? '#e6f7ff' : 'white'
        }}
      >
        <div>
          <Title level={5} style={{ margin: 0 }}>
            {section.title}
          </Title>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Grid Layout • {section.cols} columns • {section.items.length} widgets
          </Text>
        </div>
        
        {isSelected && (
          <Space>
            <Tooltip title="Section Settings" placement="top">
              <Button
                size="small"
                type="text"
                icon={<SettingOutlined style={{ fontSize: 16, color: '#1890ff' }} />}
                style={{
                  width: 32,
                  height: 32,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 4,
                  transition: 'all 0.2s ease'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#e6f7ff';
                  e.currentTarget.style.transform = 'scale(1.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                  e.currentTarget.style.transform = 'scale(1)';
                }}
                onClick={(e) => { e.stopPropagation(); handleEditSection(); }}
              />
            </Tooltip>
            <Popconfirm
              title="Delete this section?"
              description="All widgets in this section will be deleted."
              onConfirm={() => handleRemoveSection()}
              okText="Delete"
              cancelText="Cancel"
              placement="topRight"
              okType="danger"
            >
              <Button
                size="small"
                type="text"
                danger
                icon={<DeleteOutlined style={{ fontSize: 16, color: '#ff4d4f' }} />}
                style={{
                  width: 32,
                  height: 32,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 4,
                  transition: 'all 0.2s ease'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#fff2f0';
                  e.currentTarget.style.transform = 'scale(1.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                  e.currentTarget.style.transform = 'scale(1)';
                }}
                onClick={(e) => e.stopPropagation()}
              />
            </Popconfirm>
          </Space>
        )}
      </div>

      {/* Grid Content */}
      <div ref={setNodeRef} style={{ minHeight: 200 }}>
        {section.items.length > 0 ? (
          <ReactGridLayout
            className="layout"
            layout={layouts}
            cols={section.cols}
            rowHeight={section.rowHeight}
            onLayoutChange={handleLayoutChange}
            isDraggable={!previewMode}
            isResizable={!previewMode}
            margin={[8, 8]}
            containerPadding={[16, 16]}
            breakpoints={section.breakpoints}
            dragHandleClassName="react-grid-dragHandle"
          >
            {section.items.map(renderGridItem)}
          </ReactGridLayout>
        ) : (
          <div style={{ 
            padding: '60px 20px',
            textAlign: 'center',
            background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
            borderRadius: 12,
            margin: 16
          }}>
            <div style={{ marginBottom: 24 }}>
              <AppstoreOutlined style={{ 
                fontSize: 48, 
                color: '#1890ff',
                marginBottom: 16 
              }} />
              <div style={{ 
                fontSize: 16, 
                fontWeight: 600, 
                color: '#262626',
                marginBottom: 8 
              }}>
                Empty Grid Section
              </div>
              <Text type="secondary" style={{ fontSize: 14 }}>
                Start building your form by adding widgets to this section
              </Text>
            </div>
            
            <Space direction="vertical" size="middle">
              <Button 
                type="primary" 
                size="large"
                icon={<PlusOutlined />}
                onClick={() => setShowWidgetModal(true)}
                style={{
                  borderRadius: 8,
                  height: 44,
                  paddingLeft: 24,
                  paddingRight: 24,
                  fontWeight: 500,
                  boxShadow: '0 4px 12px rgba(24, 144, 255, 0.3)'
                }}
              >
                Add Your First Widget
              </Button>
              
              <div style={{ 
                fontSize: 12, 
                color: '#8c8c8c',
                marginTop: 12 
              }}>
                💡 You can also drag widgets from the sidebar
              </div>
            </Space>
          </div>
        )}
      </div>

      {/* Enhanced Widget Selection Modal */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <AppstoreOutlined style={{ color: '#1890ff' }} />
            <span>Add Widget to Section</span>
          </div>
        }
        open={showWidgetModal}
        onCancel={() => {
          setShowWidgetModal(false);
          setSearchTerm('');
          setSelectedCategory('form');
        }}
        footer={null}
        width={800}
        style={{ top: 50 }}
      >
        <div style={{ marginBottom: 20 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search widgets..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ marginBottom: 16 }}
            allowClear
          />
          
          <Tabs 
            activeKey={selectedCategory} 
            onChange={setSelectedCategory}
            type="card"
            size="small"
          >
            {Object.entries(WIDGET_CATEGORIES).map(([categoryKey, category]) => (
              <Tabs.TabPane
                tab={
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span>{category.title}</span>
                    <span style={{ 
                      background: '#f0f0f0', 
                      borderRadius: 10, 
                      padding: '2px 6px', 
                      fontSize: 10,
                      color: '#666'
                    }}>
                      {category.widgets.length}
                    </span>
                  </div>
                }
                key={categoryKey}
              >
                <div style={{ maxHeight: '50vh', overflowY: 'auto', padding: '8px 0' }}>
                  <Row gutter={[12, 12]}>
                    {category.widgets
                      .filter(widget => 
                        searchTerm === '' || 
                        widget.label.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        widget.type.toLowerCase().includes(searchTerm.toLowerCase())
                      )
                      .map((widget) => (
                        <Col xs={12} sm={8} md={6} key={widget.type}>
                          <Card 
                            hoverable
                            size="small"
                            onClick={() => handleAddWidget(widget.type as WidgetType)}
                            style={{ 
                              textAlign: 'center', 
                              cursor: 'pointer',
                              border: '1px solid #e8e8e8',
                              transition: 'all 0.2s ease',
                              height: 88
                            }}
                            bodyStyle={{ 
                              padding: '12px 8px',
                              display: 'flex',
                              flexDirection: 'column',
                              alignItems: 'center',
                              justifyContent: 'center',
                              height: '100%'
                            }}
                            onMouseEnter={(e) => {
                              e.currentTarget.style.borderColor = '#1890ff';
                              e.currentTarget.style.boxShadow = '0 4px 12px rgba(24, 144, 255, 0.15)';
                            }}
                            onMouseLeave={(e) => {
                              e.currentTarget.style.borderColor = '#e8e8e8';
                              e.currentTarget.style.boxShadow = 'none';
                            }}
                          >
                            <div style={{ 
                              fontSize: '20px', 
                              marginBottom: 6,
                              lineHeight: 1
                            }}>
                              {widget.icon}
                            </div>
                            <div style={{ 
                              fontSize: '11px', 
                              fontWeight: 500,
                              color: '#262626',
                              lineHeight: 1.2
                            }}>
                              {widget.label}
                            </div>
                          </Card>
                        </Col>
                      ))}
                  </Row>
                  
                  {category.widgets.filter(widget => 
                    searchTerm === '' || 
                    widget.label.toLowerCase().includes(searchTerm.toLowerCase()) ||
                    widget.type.toLowerCase().includes(searchTerm.toLowerCase())
                  ).length === 0 && (
                    <Empty 
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description={searchTerm ? `No widgets found for "${searchTerm}"` : "No widgets in this category"}
                      style={{ margin: '40px 0' }}
                    />
                  )}
                </div>
              </Tabs.TabPane>
            ))}
          </Tabs>
        </div>
        
        <div style={{
          borderTop: '1px solid #f0f0f0',
          paddingTop: 16,
          marginTop: 16,
          textAlign: 'center'
        }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            💡 Click on any widget to add it to your form section
          </Text>
        </div>
      </Modal>
    </Card>
  );
};

export default GridSection;
