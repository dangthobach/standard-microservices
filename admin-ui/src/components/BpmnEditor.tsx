import React, { useEffect, useRef, useState } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import { 
  Card, 
  Button, 
  Space, 
  message, 
  Modal, 
  Input, 
  Form, 
  Select, 
  Upload, 
  Typography,
  Divider,
  Row,
  Col,
  Tooltip
} from 'antd';
import { 
  SaveOutlined, 
  DownloadOutlined, 
  UploadOutlined, 
  FileAddOutlined,
  UndoOutlined,
  RedoOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  ExpandOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import type { UploadProps } from 'antd';
import '../styles/bpmn-editor.css';

const { Title, Text } = Typography;
const { TextArea } = Input;
const { Option } = Select;

interface BpmnEditorProps {
  initialXml?: string;
  processKey?: string;
  processName?: string;
  onSave?: (xml: string, processKey: string, processName: string) => void;
  readonly?: boolean;
}

const BpmnEditor: React.FC<BpmnEditorProps> = ({
  initialXml,
  processKey = '',
  processName = '',
  onSave,
  readonly = false
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const modelerRef = useRef<BpmnModeler | null>(null);
  const [currentXml, setCurrentXml] = useState<string>('');
  const [saveModalVisible, setSaveModalVisible] = useState(false);
  const [propertiesModalVisible, setPropertiesModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  // Default BPMN XML template
  const defaultXml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="Examples">
  <process id="newProcess" name="New Process" isExecutable="true">
    <startEvent id="start" name="Start">
      <outgoing>flow1</outgoing>
    </startEvent>
    <sequenceFlow id="flow1" sourceRef="start" targetRef="end"/>
    <endEvent id="end" name="End">
      <incoming>flow1</incoming>
    </endEvent>
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_newProcess">
    <bpmndi:BPMNPlane id="BPMNPlane_newProcess" bpmnElement="newProcess">
      <bpmndi:BPMNShape id="BPMNShape_start" bpmnElement="start">
        <omgdc:Bounds x="100" y="100" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <omgdc:Bounds x="105" y="143" width="26" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="BPMNShape_end" bpmnElement="end">
        <omgdc:Bounds x="300" y="100" width="36" height="36"/>
        <bpmndi:BPMNLabel>
          <omgdc:Bounds x="308" y="143" width="20" height="14"/>
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="BPMNEdge_flow1" bpmnElement="flow1">
        <omgdi:waypoint x="136" y="118"/>
        <omgdi:waypoint x="300" y="118"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`;

  useEffect(() => {
    if (!containerRef.current) return;

    // Initialize BPMN Modeler
    const modeler = new BpmnModeler({
      container: containerRef.current,
      width: '100%',
      height: '600px',
      additionalModules: [
        // Add additional modules if needed
      ]
    });

    modelerRef.current = modeler;

    // Load initial XML or default template
    const xmlToLoad = initialXml || defaultXml;
    
    modeler
      .importXML(xmlToLoad)
      .then(() => {
        const canvas = modeler.get('canvas') as any;
        canvas.zoom('fit-viewport');
        setCurrentXml(xmlToLoad);
      })
      .catch((err: any) => {
        console.error('Error loading BPMN:', err);
        // Try to load default template if initialXml failed
        if (initialXml && initialXml !== defaultXml) {
          console.log('Trying to load default template...');
          modeler
            .importXML(defaultXml)
            .then(() => {
              const canvas = modeler.get('canvas') as any;
              canvas.zoom('fit-viewport');
              setCurrentXml(defaultXml);
            })
            .catch((defaultErr: any) => {
              console.error('Error loading default BPMN:', defaultErr);
              message.error('Failed to load BPMN diagram');
            });
        } else {
          message.error('Failed to load BPMN diagram');
        }
      });

    // Listen for changes
    modeler.on('commandStack.changed', async () => {
      try {
        const { xml } = await modeler.saveXML({ format: true });
        if (xml) {
          setCurrentXml(xml);
        }
      } catch (err) {
        console.error('Error getting XML:', err);
      }
    });

    return () => {
      if (modelerRef.current) {
        modelerRef.current.destroy();
      }
    };
  }, [initialXml, defaultXml]);

  const handleSave = async () => {
    if (!modelerRef.current) return;

    try {
      const { xml } = await modelerRef.current.saveXML({ format: true });
      if (xml) {
        setCurrentXml(xml);
        setSaveModalVisible(true);
        
        // Pre-fill form with current values
        form.setFieldsValue({
          processKey: processKey || extractProcessKey(xml),
          processName: processName || extractProcessName(xml),
          description: '',
          category: 'workflow'
        });
      }
    } catch (err) {
      message.error('Failed to save BPMN');
      console.error('Error saving BPMN:', err);
    }
  };

  const handleSaveConfirm = async (values: any) => {
    setLoading(true);
    try {
      // Update XML with new process key and name
      const updatedXml = updateProcessMetadata(currentXml, values.processKey, values.processName);
      
      if (onSave) {
        await onSave(updatedXml, values.processKey, values.processName);
      }
      
      message.success('BPMN process saved successfully');
      setSaveModalVisible(false);
      form.resetFields();
    } catch (error) {
      message.error('Failed to save process');
      console.error('Error saving process:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    if (!modelerRef.current) return;

    try {
      const { xml } = await modelerRef.current.saveXML({ format: true });
      
      if (xml) {
        // Create and download file
        const blob = new Blob([xml], { type: 'application/xml' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${processKey || 'process'}.bpmn`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        
        message.success('BPMN file exported successfully');
      }
    } catch (err) {
      message.error('Failed to export BPMN');
      console.error('Error exporting BPMN:', err);
    }
  };

  const handleImport = async (file: File) => {
    const reader = new FileReader();
    reader.onload = async (e) => {
      const xml = e.target?.result as string;
      if (modelerRef.current) {
        try {
          await modelerRef.current.importXML(xml);
          const canvas = modelerRef.current.get('canvas') as any;
          canvas.zoom('fit-viewport');
          setCurrentXml(xml);
          message.success('BPMN file imported successfully');
        } catch (err) {
          message.error('Failed to import BPMN file');
          console.error('Error importing BPMN:', err);
        }
      }
    };
    reader.readAsText(file);
    return false; // Prevent upload
  };

  const handleNewDiagram = () => {
    Modal.confirm({
      title: 'Create New Diagram',
      content: 'Are you sure you want to create a new diagram? Current changes will be lost.',
      onOk: async () => {
        if (modelerRef.current) {
          try {
            await modelerRef.current.importXML(defaultXml);
            const canvas = modelerRef.current.get('canvas') as any;
            canvas.zoom('fit-viewport');
            setCurrentXml(defaultXml);
            message.success('New diagram created');
          } catch (err) {
            message.error('Failed to create new diagram');
          }
        }
      }
    });
  };

  const handleUndo = () => {
    if (modelerRef.current) {
      const commandStack = modelerRef.current.get('commandStack') as any;
      commandStack.undo();
    }
  };

  const handleRedo = () => {
    if (modelerRef.current) {
      const commandStack = modelerRef.current.get('commandStack') as any;
      commandStack.redo();
    }
  };

  const handleZoomIn = () => {
    if (modelerRef.current) {
      const canvas = modelerRef.current.get('canvas') as any;
      canvas.zoom(canvas.zoom() + 0.1);
    }
  };

  const handleZoomOut = () => {
    if (modelerRef.current) {
      const canvas = modelerRef.current.get('canvas') as any;
      canvas.zoom(canvas.zoom() - 0.1);
    }
  };

  const handleZoomFit = () => {
    if (modelerRef.current) {
      const canvas = modelerRef.current.get('canvas') as any;
      canvas.zoom('fit-viewport');
    }
  };

  const handleShowProperties = () => {
    setPropertiesModalVisible(true);
  };

  const extractProcessKey = (xml: string): string => {
    const match = xml.match(/process id="([^"]+)"/);
    return match ? match[1] : 'newProcess';
  };

  const extractProcessName = (xml: string): string => {
    const match = xml.match(/process id="[^"]+" name="([^"]+)"/);
    return match ? match[1] : 'New Process';
  };

  const updateProcessMetadata = (xml: string, key: string, name: string): string => {
    return xml
      .replace(/process id="[^"]+"/, `process id="${key}"`)
      .replace(/process id="[^"]+" name="[^"]+"/, `process id="${key}" name="${name}"`);
  };

  const uploadProps: UploadProps = {
    accept: '.bpmn,.xml',
    beforeUpload: handleImport,
    showUploadList: false,
  };

  return (
    <div style={{ padding: '24px' }}>
      <Title level={2}>BPMN Process Designer</Title>
      
      {/* Toolbar */}
      <Card style={{ marginBottom: '16px' }}>
        <Row gutter={16} align="middle">
          <Col>
            <Space wrap>
              <Button 
                icon={<FileAddOutlined />} 
                onClick={handleNewDiagram}
                disabled={readonly}
              >
                New
              </Button>
              
              <Upload {...uploadProps}>
                <Button 
                  icon={<UploadOutlined />}
                  disabled={readonly}
                >
                  Import
                </Button>
              </Upload>
              
              <Button 
                type="primary" 
                icon={<SaveOutlined />} 
                onClick={handleSave}
                disabled={readonly}
              >
                Save
              </Button>
              
              <Button 
                icon={<DownloadOutlined />} 
                onClick={handleExport}
              >
                Export
              </Button>
            </Space>
          </Col>
          
          <Col>
            <Divider type="vertical" />
          </Col>
          
          <Col>
            <Space>
              <Tooltip title="Undo">
                <Button 
                  icon={<UndoOutlined />} 
                  onClick={handleUndo}
                  disabled={readonly}
                />
              </Tooltip>
              
              <Tooltip title="Redo">
                <Button 
                  icon={<RedoOutlined />} 
                  onClick={handleRedo}
                  disabled={readonly}
                />
              </Tooltip>
            </Space>
          </Col>
          
          <Col>
            <Divider type="vertical" />
          </Col>
          
          <Col>
            <Space>
              <Tooltip title="Zoom In">
                <Button 
                  icon={<ZoomInOutlined />} 
                  onClick={handleZoomIn}
                />
              </Tooltip>
              
              <Tooltip title="Zoom Out">
                <Button 
                  icon={<ZoomOutOutlined />} 
                  onClick={handleZoomOut}
                />
              </Tooltip>
              
              <Tooltip title="Fit to View">
                <Button 
                  icon={<ExpandOutlined />} 
                  onClick={handleZoomFit}
                />
              </Tooltip>
            </Space>
          </Col>
          
          <Col>
            <Divider type="vertical" />
          </Col>
          
          <Col>
            <Button 
              icon={<InfoCircleOutlined />} 
              onClick={handleShowProperties}
            >
              Properties
            </Button>
          </Col>
        </Row>
      </Card>

      {/* BPMN Canvas */}
      <Card>
        <div
          ref={containerRef}
          style={{
            width: '100%',
            height: '600px',
            border: '1px solid #d9d9d9',
            borderRadius: '6px',
          }}
        />
      </Card>

      {/* Save Modal */}
      <Modal
        title="Save Process"
        open={saveModalVisible}
        onCancel={() => setSaveModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveConfirm}
        >
          <Form.Item
            name="processKey"
            label="Process Key"
            rules={[
              { required: true, message: 'Please enter process key' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: 'Invalid process key format' }
            ]}
          >
            <Input placeholder="e.g., orderProcess" />
          </Form.Item>
          
          <Form.Item
            name="processName"
            label="Process Name"
            rules={[{ required: true, message: 'Please enter process name' }]}
          >
            <Input placeholder="e.g., Order Processing" />
          </Form.Item>
          
          <Form.Item
            name="description"
            label="Description"
          >
            <TextArea 
              rows={3} 
              placeholder="Describe what this process does..."
            />
          </Form.Item>
          
          <Form.Item
            name="category"
            label="Category"
            initialValue="workflow"
          >
            <Select>
              <Option value="workflow">Workflow</Option>
              <Option value="approval">Approval</Option>
              <Option value="integration">Integration</Option>
              <Option value="automation">Automation</Option>
              <Option value="other">Other</Option>
            </Select>
          </Form.Item>
          
          <Form.Item>
            <Space>
              <Button 
                type="primary" 
                htmlType="submit" 
                loading={loading}
                icon={<SaveOutlined />}
              >
                Save Process
              </Button>
              <Button onClick={() => setSaveModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Properties Modal */}
      <Modal
        title="Process Properties"
        open={propertiesModalVisible}
        onCancel={() => setPropertiesModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setPropertiesModalVisible(false)}>
            Close
          </Button>
        ]}
        width={700}
      >
        <div>
          <Title level={4}>Current Process Information</Title>
          <Row gutter={16}>
            <Col span={12}>
              <Text strong>Process Key:</Text>
              <br />
              <Text code>{extractProcessKey(currentXml)}</Text>
            </Col>
            <Col span={12}>
              <Text strong>Process Name:</Text>
              <br />
              <Text>{extractProcessName(currentXml)}</Text>
            </Col>
          </Row>
          
          <Divider />
          
          <Title level={4}>XML Source</Title>
          <TextArea
            value={currentXml}
            rows={15}
            readOnly
            style={{ fontFamily: 'monospace', fontSize: '12px' }}
          />
        </div>
      </Modal>
    </div>
  );
};

export default BpmnEditor;
