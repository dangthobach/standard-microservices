import React, { useCallback } from 'react';
import { Layout, Button, Space, Typography, message, Empty, Tooltip, Divider } from 'antd';
import { 
  EyeOutlined, 
  EditOutlined, 
  SaveOutlined, 
  UndoOutlined, 
  RedoOutlined,
  PlusOutlined,
  CopyOutlined,
  ScissorOutlined,
  SnippetsOutlined,
  DeleteOutlined
} from '@ant-design/icons';

import { usePageBuilderStore } from '../../store/pageBuilderStore';
import { SectionType } from '../../types/pageBuilder';
import GridSection from './GridSection';
import StackSection from './StackSection';

const { Content } = Layout;
const { Title } = Typography;

interface PageBuilderCanvasProps {
  onSave?: (schema: any) => void;
}

const PageBuilderCanvas: React.FC<PageBuilderCanvasProps> = ({ onSave }) => {
  const {
    schema,
    previewMode,
    selectedSectionId,
    setPreviewMode,
    addGridSection,
    addStackSection,
    selectItem,
    undo,
    redo,
    canUndo,
    canRedo
  } = usePageBuilderStore();

  const handleSave = useCallback(() => {
    if (onSave) {
      onSave(schema);
      message.success('Form saved successfully!');
    } else {
      // Default save behavior - could save to localStorage or make API call
      localStorage.setItem('pageBuilderSchema', JSON.stringify(schema));
      message.success('Form saved to browser storage!');
    }
  }, [schema, onSave]);

  const handlePreviewToggle = useCallback(() => {
    setPreviewMode(!previewMode);
    if (!previewMode) {
      selectItem(undefined, undefined);
    }
  }, [previewMode, setPreviewMode, selectItem]);

  const handleUndo = useCallback(() => {
    undo();
  }, [undo]);

  const handleRedo = useCallback(() => {
    redo();
  }, [redo]);

  const renderSection = (section: SectionType) => {
    const isSelected = selectedSectionId === section.id;
    
    if (section.type === 'grid') {
      return (
        <GridSection
          key={section.id}
          section={section as any}
          isSelected={isSelected}
          previewMode={previewMode}
        />
      );
    } else if (section.type === 'stack') {
      return (
        <StackSection
          key={section.id}
          section={section as any}
          isSelected={isSelected}
          previewMode={previewMode}
        />
      );
    }
    
    return null;
  };

  const canvasStyle: React.CSSProperties = {
    minHeight: '100vh',
    background: previewMode ? '#f5f5f5' : '#fafafa',
    padding: previewMode ? 0 : 24,
    transition: 'all 0.3s ease'
  };

  return (
    <Layout style={{ height: '100vh' }}>
      {/* Toolbar */}
      {!previewMode && (
        <div 
          style={{
            background: 'white',
            padding: '16px 24px',
            borderBottom: '1px solid #f0f0f0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}
        >
          <div>
            <Title level={4} style={{ margin: 0 }}>
              {schema.title || 'Untitled Form'}
            </Title>
          </div>
          
          <Space>
            <Tooltip title="Undo (Ctrl+Z)">
              <Button 
                icon={<UndoOutlined />} 
                disabled={!canUndo()}
                onClick={handleUndo}
              >
                Undo
              </Button>
            </Tooltip>
            <Tooltip title="Redo (Ctrl+Y)">
              <Button 
                icon={<RedoOutlined />} 
                disabled={!canRedo()}
                onClick={handleRedo}
              >
                Redo
              </Button>
            </Tooltip>
            <Divider type="vertical" />
            <Tooltip title="Copy (Ctrl+C)">
              <Button 
                icon={<CopyOutlined />}
                disabled={!selectedSectionId}
                onClick={() => message.info('Copy functionality coming soon!')}
              >
                Copy
              </Button>
            </Tooltip>
            <Tooltip title="Cut (Ctrl+X)">
              <Button 
                icon={<ScissorOutlined />}
                disabled={!selectedSectionId}
                onClick={() => message.info('Cut functionality coming soon!')}
              >
                Cut
              </Button>
            </Tooltip>
            <Tooltip title="Paste (Ctrl+V)">
              <Button 
                icon={<SnippetsOutlined />}
                disabled={true}
                onClick={() => message.info('Paste functionality coming soon!')}
              >
                Paste
              </Button>
            </Tooltip>
            <Tooltip title="Delete (Del)">
              <Button 
                icon={<DeleteOutlined />}
                disabled={!selectedSectionId}
                onClick={() => message.info('Delete functionality coming soon!')}
                danger
              >
                Delete
              </Button>
            </Tooltip>
            <Divider type="vertical" />
            <Tooltip title="Save (Ctrl+S)">
              <Button 
                icon={<SaveOutlined />} 
                type="primary"
                onClick={handleSave}
              >
                Save
              </Button>
            </Tooltip>
            <Tooltip title="Preview Mode">
              <Button 
                icon={previewMode ? <EditOutlined /> : <EyeOutlined />}
                onClick={handlePreviewToggle}
              >
                {previewMode ? 'Edit' : 'Preview'}
              </Button>
            </Tooltip>
          </Space>
        </div>
      )}

      {/* Canvas Content */}
      <Content style={canvasStyle}>
        {schema.sections.length > 0 ? (
          <div style={{ 
            maxWidth: previewMode ? 800 : '100%',
            margin: previewMode ? '0 auto' : 0,
            padding: previewMode ? 24 : 0,
            background: previewMode ? 'white' : 'transparent',
            borderRadius: previewMode ? 8 : 0,
            boxShadow: previewMode ? '0 2px 8px rgba(0,0,0,0.1)' : 'none',
            gap: 24,
            display: 'flex',
            flexDirection: 'column'
          }}>
            {previewMode && (
              <div style={{ textAlign: 'center', marginBottom: 32 }}>
                <Title level={2}>{schema.title}</Title>
                {schema.description && (
                  <Typography.Paragraph type="secondary">
                    {schema.description}
                  </Typography.Paragraph>
                )}
              </div>
            )}
            
            {schema.sections.map(renderSection)}
          </div>
        ) : (
          <Empty
            style={{ marginTop: '20vh' }}
            description="Start building your form by adding sections"
          >
            <Space>
              <Button 
                type="primary" 
                icon={<PlusOutlined />}
                onClick={() => addGridSection('New Grid Section', 12)}
              >
                Add Grid Section
              </Button>
              <Button 
                icon={<PlusOutlined />}
                onClick={() => addStackSection('New Stack Section', 'column')}
              >
                Add Stack Section
              </Button>
            </Space>
          </Empty>
        )}
      </Content>

      {/* Preview Mode Exit */}
      {previewMode && (
        <div 
          style={{
            position: 'fixed',
            top: 20,
            right: 20,
            zIndex: 1000
          }}
        >
          <Button
            size="large"
            type="primary"
            icon={<EditOutlined />}
            onClick={handlePreviewToggle}
          >
            Back to Edit
          </Button>
        </div>
      )}
    </Layout>
  );
};

export default PageBuilderCanvas;
