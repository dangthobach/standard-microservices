import React, { useState } from 'react';
import { Card, Input, Button, List, Tag, Space, DatePicker } from 'antd';

const LogsViewer: React.FC = () => {
    const [logs, setLogs] = useState<any[]>([
        { id: 1, timestamp: '2023-10-27 10:00:01', level: 'INFO', service: 'iam-service', message: 'User login successful: admin' },
        { id: 2, timestamp: '2023-10-27 10:05:22', level: 'WARN', service: 'business-service', message: 'Product stock low: SKU-123' },
        { id: 3, timestamp: '2023-10-27 10:10:15', level: 'ERROR', service: 'integration-service', message: 'Connection timeout: Connector A' },
    ]);

    const handleSearch = (value: string) => {
        // Mock search
        console.log('Searching logs for:', value);
    };

    return (
        <div style={{ padding: 24 }}>
            <h2>System Logs</h2>
            <Space style={{ marginBottom: 16 }}>
                <Input.Search placeholder="Search logs..." onSearch={handleSearch} allowClear />
                <DatePicker.RangePicker />
                <Button type="primary">Refresh</Button>
            </Space>

            <List
                grid={{ gutter: 16, column: 1 }}
                dataSource={logs}
                renderItem={item => (
                    <List.Item>
                        <Card size="small">
                            <Space>
                                <Tag color="blue">{item.timestamp}</Tag>
                                <Tag color={item.level === 'ERROR' ? 'red' : item.level === 'WARN' ? 'orange' : 'green'}>{item.level}</Tag>
                                <Tag color="purple">{item.service}</Tag>
                                <span>{item.message}</span>
                            </Space>
                        </Card>
                    </List.Item>
                )}
            />
        </div>
    );
};

export default LogsViewer;
