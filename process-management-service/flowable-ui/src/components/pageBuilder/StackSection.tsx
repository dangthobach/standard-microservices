import React, { useCallback } from 'react';
import { Card, Typography, Button, Space, Popconfirm, Tooltip, Empty } from 'antd';
import { useDroppable } from '@dnd-kit/core';
import { 
  DeleteOutlined, 
  CopyOutlined, 
  SettingOutlined,
  PlusOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons';
import { StackSection as StackSectionType, StackItem } from '../../types/pageBuilder';
import { usePageBuilderStore } from '../../store/pageBuilderStore';
import WidgetRenderer from './WidgetRenderer';

const { Title, Text } = Typography;

interface StackSectionProps {
  section: StackSectionType;
  isSelected?: boolean;
  previewMode?: boolean;
}

const StackSection: React.FC<StackSectionProps> = ({ 
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
    selectedSectionId
  } = usePageBuilderStore();

  // Setup drop zone for dragging widgets into stack
  const { setNodeRef, isOver } = useDroppable({
    id: `stack-section-${section.id}`,
    data: {
      type: 'stack-section',
      sectionId: section.id,
      accepts: ['widget']
    }
  });

  const handleItemClick = useCallback((itemId: string, e: React.MouseEvent) => {
    if (previewMode) return;
    e.stopPropagation();
    selectItem(itemId, section.id);
  }, [selectItem, section.id, previewMode]);

  const handleSectionClick = useCallback((e: React.MouseEvent) => {
    if (previewMode) return;
    e.stopPropagation();
    selectItem(undefined, section.id);
  }, [selectItem, section.id, previewMode]);

  const handleRemoveSection = useCallback(() => {
    removeSection(section.id);
  }, [removeSection, section.id]);

  const handleEditSection = useCallback(() => {
    selectItem(undefined, section.id);
  }, [selectItem, section.id]);

  const handleRemoveItem = useCallback((itemId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    removeItem(section.id, itemId);
  }, [removeItem, section.id]);

  const handleDuplicateItem = useCallback((itemId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    duplicateItem(section.id, itemId);
  }, [duplicateItem, section.id]);

  const handleMoveItemUp = useCallback((itemId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const item = section.children.find(item => item.id === itemId);
    if (item && item.order > 0) {
      updateItem(section.id, itemId, { order: item.order - 1 });
    }
  }, [section.children, updateItem, section.id]);

  const handleMoveItemDown = useCallback((itemId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const item = section.children.find(item => item.id === itemId);
    if (item && item.order < section.children.length - 1) {
      updateItem(section.id, itemId, { order: item.order + 1 });
    }
  }, [section.children, updateItem, section.id]);

  const renderStackItem = useCallback((item: StackItem) => {
    const isItemSelected = selectedItemId === item.id && selectedSectionId === section.id;
    
    return (
      <div
        key={item.id}
        onClick={(e) => handleItemClick(item.id, e)}
        style={{
          position: 'relative',
          border: isItemSelected ? '2px solid #1890ff' : '1px solid transparent',
          borderRadius: 4,
          overflow: 'hidden',
          background: 'white',
          minHeight: section.direction === 'row' ? 'auto' : 60
        }}
      >
        {/* Widget Content */}
        <div style={{ padding: 8 }}>
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
              top: 4,
              right: 4,
              background: 'white',
              border: '1px solid #d9d9d9',
              borderRadius: 6,
              padding: '6px',
              display: 'flex',
              gap: 4,
              boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
              zIndex: 10
            }}
          >
            <Tooltip title="Move Up" placement="top">
              <Button
                size="small"
                type="text"
                icon={<ArrowUpOutlined style={{ fontSize: 16, color: '#722ed1' }} />}
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
                  e.currentTarget.style.background = '#f9f0ff';
                  e.currentTarget.style.transform = 'scale(1.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                  e.currentTarget.style.transform = 'scale(1)';
                }}
                onClick={(e) => handleMoveItemUp(item.id, e)}
                disabled={item.order === 0}
              />
            </Tooltip>
            <Tooltip title="Move Down" placement="top">
              <Button
                size="small"
                type="text"
                icon={<ArrowDownOutlined style={{ fontSize: 16, color: '#722ed1' }} />}
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
                  e.currentTarget.style.background = '#f9f0ff';
                  e.currentTarget.style.transform = 'scale(1.1)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'transparent';
                  e.currentTarget.style.transform = 'scale(1)';
                }}
                onClick={(e) => handleMoveItemDown(item.id, e)}
                disabled={item.order === section.children.length - 1}
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
                onConfirm={(e) => handleRemoveItem(item.id, e!)}
                okText="Delete"
                cancelText="Cancel"
                placement="topRight"
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
      </div>
    );
  }, [selectedItemId, selectedSectionId, section, handleItemClick, handleDuplicateItem, handleRemoveItem, handleMoveItemUp, handleMoveItemDown, previewMode]);

  // Sort children by order
  const sortedChildren = [...section.children].sort((a, b) => a.order - b.order);

  const sectionStyle: React.CSSProperties = {
    border: isSelected ? '2px solid #1890ff' : '1px solid #d9d9d9',
    borderRadius: 8,
    background: previewMode ? 'white' : (isOver ? '#f0f9ff' : '#fafafa'),
    minHeight: previewMode ? 'auto' : 120,
    position: 'relative',
    transition: 'all 0.2s ease'
  };

  const stackContentStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: section.direction,
    alignItems: section.align,
    justifyContent: section.justify,
    gap: section.gap,
    flexWrap: section.wrap ? 'wrap' : 'nowrap',
    padding: 16,
    minHeight: previewMode ? 'auto' : 100
  };

  if (previewMode) {
    return (
      <div style={sectionStyle}>
        {section.children.length > 0 && (
          <div style={stackContentStyle}>
            {sortedChildren.map(renderStackItem)}
          </div>
        )}
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
            Stack Layout • {section.direction} • {section.children.length} widgets
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
              onConfirm={(e) => { e?.stopPropagation(); handleRemoveSection(); }}
              okText="Delete"
              cancelText="Cancel"
              placement="topRight"
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

      {/* Stack Content */}
      <div ref={setNodeRef} style={{ minHeight: 100 }}>
        {section.children.length > 0 ? (
          <div style={stackContentStyle}>
            {sortedChildren.map(renderStackItem)}
          </div>
        ) : (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Drag widgets here to build your form"
            style={{ padding: '40px 20px' }}
          >
            <Button type="dashed" icon={<PlusOutlined />}>
              Add Widget
            </Button>
          </Empty>
        )}
      </div>
    </Card>
  );
};

export default StackSection;
