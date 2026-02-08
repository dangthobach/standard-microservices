import React from 'react';
import {
  Input,
  InputNumber,
  Select,
  Radio,
  Checkbox,
  DatePicker,
  TimePicker,
  Switch,
  Slider,
  Rate,
  Upload,
  Cascader,
  TreeSelect,
  Transfer,
  Image,
  Typography,
  Divider,
  Space,
  Card,
  Tabs,
  Collapse,
  Steps,
  List,
  Table,
  Button,
  Form
} from 'antd';
import { UploadOutlined, InboxOutlined } from '@ant-design/icons';
import { WidgetType } from '../../types/pageBuilder';

const { TextArea } = Input;
const { Text, Title } = Typography;
const { Panel } = Collapse;
const { TabPane } = Tabs;

interface Widget {
  id: string;
  type: WidgetType;
  props: any;
}

interface WidgetRendererProps {
  widget: Widget;
  mode: 'builder' | 'preview';
  value?: any;
  onChange?: (value: any) => void;
}

const WidgetRenderer: React.FC<WidgetRendererProps> = ({ 
  widget, 
  mode = 'builder',
  value,
  onChange 
}) => {
  const { type, props } = widget;

  // Common form item props for builder mode
  const formItemProps = mode === 'builder' ? {
    style: { marginBottom: 0 }
  } : {};

  const commonProps = {
    ...props,
    value,
    onChange,
    disabled: mode === 'builder' && !props.interactive,
    placeholder: mode === 'builder' ? props.placeholder || `${props.label}...` : props.placeholder
  };

  switch (type) {
    case 'text-field':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <Input {...commonProps} />
        </Form.Item>
      );

    case 'textarea':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <TextArea {...commonProps} rows={props.rows || 4} />
        </Form.Item>
      );

    case 'number':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <InputNumber {...commonProps} style={{ width: '100%' }} min={props.min} max={props.max} />
        </Form.Item>
      );

    case 'email':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <Input {...commonProps} type="email" />
        </Form.Item>
      );

    case 'password':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <Input.Password {...commonProps} />
        </Form.Item>
      );

    case 'select':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <Select {...commonProps} options={props.options || []}>
            {props.options?.map((option: any) => (
              <Select.Option key={option.value} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
      );

    case 'radio':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <Radio.Group {...commonProps} options={props.options || []} />
        </Form.Item>
      );

    case 'checkbox':
      return (
        <Form.Item label={props.label} {...formItemProps}>
          <Checkbox.Group {...commonProps} options={props.options || []} />
        </Form.Item>
      );

    case 'date':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <DatePicker {...commonProps} style={{ width: '100%' }} />
        </Form.Item>
      );

    case 'time':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <TimePicker {...commonProps} style={{ width: '100%' }} />
        </Form.Item>
      );

    case 'switch':
      return (
        <Form.Item label={props.label} {...formItemProps}>
          <Switch {...commonProps} checkedChildren={props.checkedChildren} unCheckedChildren={props.unCheckedChildren} />
        </Form.Item>
      );

    case 'slider':
      return (
        <Form.Item label={props.label} {...formItemProps}>
          <Slider {...commonProps} min={props.min} max={props.max} />
        </Form.Item>
      );

    case 'rate':
      return (
        <Form.Item label={props.label} {...formItemProps}>
          <Rate {...commonProps} allowHalf={props.allowHalf} character={props.character} />
        </Form.Item>
      );

    case 'upload':
      const uploadProps = {
        ...commonProps,
        accept: props.accept,
        multiple: props.multiple,
        listType: props.listType || 'text'
      };

      const uploadContent = props.listType === 'picture-card' ? (
        <div>
          <UploadOutlined />
          <div style={{ marginTop: 8 }}>Upload</div>
        </div>
      ) : props.listType === 'dragger' ? (
        <Upload.Dragger {...uploadProps}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">Click or drag file to this area to upload</p>
        </Upload.Dragger>
      ) : (
        <Upload {...uploadProps}>
          <Button icon={<UploadOutlined />}>Click to Upload</Button>
        </Upload>
      );

      return (
        <Form.Item label={props.label} {...formItemProps}>
          {uploadContent}
        </Form.Item>
      );

    case 'cascader':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <Cascader {...commonProps} options={props.options || []} />
        </Form.Item>
      );

    case 'tree-select':
      return (
        <Form.Item label={props.label} required={props.required} {...formItemProps}>
          <TreeSelect {...commonProps} treeData={props.treeData || []} style={{ width: '100%' }} />
        </Form.Item>
      );

    case 'transfer':
      return (
        <Form.Item label={props.label} {...formItemProps}>
          <Transfer {...commonProps} dataSource={props.dataSource || []} titles={props.titles} />
        </Form.Item>
      );

    case 'image':
      return (
        <div style={{ textAlign: 'center' }}>
          <Image
            src={props.src || 'https://via.placeholder.com/300x200?text=Image'}
            alt={props.alt || 'Image'}
            width={props.width}
            height={props.height}
            preview={mode === 'preview'}
          />
        </div>
      );

    case 'video':
      return (
        <div style={{ textAlign: 'center' }}>
          <video
            src={props.src}
            controls={props.controls}
            width={props.width}
            height={props.height}
            style={{ maxWidth: '100%' }}
          >
            Your browser does not support the video tag.
          </video>
        </div>
      );

    case 'text-block':
      return (
        <div style={{ padding: '8px 0' }}>
          <Text
            style={{
              fontSize: props.fontSize || 14,
              color: props.color || '#000',
              fontWeight: props.fontWeight,
              textAlign: props.textAlign
            }}
          >
            {props.content || 'Text block content'}
          </Text>
        </div>
      );

    case 'divider':
      return (
        <Divider
          orientation={props.orientation}
          dashed={props.dashed}
          style={{ margin: '16px 0' }}
        >
          {props.children}
        </Divider>
      );

    case 'space':
      return (
        <Space size={props.size} direction={props.direction} wrap={props.wrap}>
          {props.children || ['Item 1', 'Item 2'].map((item, index) => (
            <div key={index}>{item}</div>
          ))}
        </Space>
      );

    case 'card':
      return (
        <Card
          title={props.title}
          bordered={props.bordered}
          hoverable={props.hoverable}
          size={props.size}
        >
          {props.children || 'Card content'}
        </Card>
      );

    case 'tabs':
      return (
        <Tabs defaultActiveKey={props.defaultActiveKey || '1'} type={props.type}>
          {(props.items || [{ key: '1', label: 'Tab 1', children: 'Content 1' }]).map((item: any) => (
            <TabPane tab={item.label} key={item.key}>
              {item.children}
            </TabPane>
          ))}
        </Tabs>
      );

    case 'collapse':
      return (
        <Collapse defaultActiveKey={props.defaultActiveKey} accordion={props.accordion}>
          {(props.items || [{ key: '1', label: 'Panel 1', children: 'Content 1' }]).map((item: any) => (
            <Panel header={item.label} key={item.key}>
              {item.children}
            </Panel>
          ))}
        </Collapse>
      );

    case 'steps':
      return (
        <Steps
          current={props.current || 0}
          direction={props.direction}
          size={props.size}
          items={props.items || [{ title: 'Step 1' }, { title: 'Step 2' }]}
        />
      );

    case 'list':
      return (
        <List
          dataSource={props.dataSource || ['Item 1', 'Item 2', 'Item 3']}
          renderItem={(item: any, index: number) => (
            <List.Item key={index}>
              {props.renderItem ? props.renderItem(item, index) : item}
            </List.Item>
          )}
          size={props.size}
          bordered={props.bordered}
        />
      );

    case 'table':
      const columns = props.columns || [
        { title: 'Name', dataIndex: 'name', key: 'name' },
        { title: 'Age', dataIndex: 'age', key: 'age' },
        { title: 'Address', dataIndex: 'address', key: 'address' }
      ];
      const dataSource = props.dataSource || [
        { key: '1', name: 'John', age: 32, address: 'New York' },
        { key: '2', name: 'Jane', age: 28, address: 'London' }
      ];

      return (
        <Table
          columns={columns}
          dataSource={dataSource}
          pagination={props.pagination}
          size={props.size}
          bordered={props.bordered}
          scroll={props.scroll}
        />
      );

    case 'chart':
      // Placeholder for chart - would integrate with actual chart library
      return (
        <div
          style={{
            width: props.width || '100%',
            height: props.height || 300,
            border: '1px dashed #d9d9d9',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#fafafa',
            borderRadius: 4
          }}
        >
          <div style={{ textAlign: 'center' }}>
            <Title level={4} type="secondary">Chart Placeholder</Title>
            <Text type="secondary">Type: {props.type || 'line'}</Text>
          </div>
        </div>
      );

    default:
      return (
        <div
          style={{
            padding: 16,
            border: '1px dashed #d9d9d9',
            borderRadius: 4,
            textAlign: 'center',
            background: '#fafafa'
          }}
        >
          <Text type="secondary">Unknown widget type: {type}</Text>
        </div>
      );
  }
};

export default WidgetRenderer;
