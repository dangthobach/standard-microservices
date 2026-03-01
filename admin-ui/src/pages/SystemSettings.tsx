import React from 'react';
import { Form, Input, Button, Switch, Divider, message } from 'antd';

const SystemSettings: React.FC = () => {
    const [form] = Form.useForm();

    const onFinish = (values: any) => {
        console.log('Success:', values);
        message.success('Settings saved successfully');
    };

    return (
        <div style={{ padding: 24, maxWidth: 800 }}>
            <h2>System Settings</h2>
            <Divider />
            <Form
                form={form}
                layout="vertical"
                onFinish={onFinish}
                initialValues={{
                    siteName: 'Enterprise Admin Portal',
                    maintenanceMode: false,
                    logLevel: 'INFO',
                    sessionTimeout: 30
                }}
            >
                <Form.Item label="Site Name" name="siteName" rules={[{ required: true }]}>
                    <Input />
                </Form.Item>

                <Form.Item label="Maintenance Mode" name="maintenanceMode" valuePropName="checked">
                    <Switch />
                </Form.Item>

                <Form.Item label="Default Log Level" name="logLevel">
                    <Input />
                </Form.Item>

                <Form.Item label="Session Timeout (minutes)" name="sessionTimeout">
                    <Input type="number" />
                </Form.Item>

                <Form.Item>
                    <Button type="primary" htmlType="submit">
                        Save Settings
                    </Button>
                </Form.Item>
            </Form>
        </div>
    );
};

export default SystemSettings;
