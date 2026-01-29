import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Layout, Menu, Typography } from 'antd';
import { 
  DashboardOutlined, 
  CheckSquareOutlined, 
  PlayCircleOutlined, 
  BarChartOutlined, 
  FileTextOutlined, 
  ExperimentOutlined, 
  DesktopOutlined,
  FormOutlined,
  TableOutlined
} from '@ant-design/icons';
import 'antd/dist/reset.css';

import Dashboard from './pages/Dashboard';
import TaskInbox from './components/TaskInbox';
import TaskDetail from './pages/TaskDetail';
import Processes from './pages/Processes';
import ProcessDesigner from './pages/ProcessDesigner';
import Monitoring from './pages/Monitoring';
import FormManagement from './pages/FormManagement';
import DmnManagement from './pages/DmnManagement';
import CaseManagement from './components/CaseManagement';
import DecisionManagement from './components/DecisionManagement';

const { Header, Content, Sider } = Layout;
const { Title } = Typography;

const App: React.FC = () => {
  const menuItems = [
    {
      key: '/',
      icon: <DashboardOutlined />,
      label: <Link to="/">Dashboard</Link>,
    },
    {
      key: '/tasks',
      icon: <CheckSquareOutlined />,
      label: <Link to="/tasks">Task Inbox</Link>,
    },
    {
      key: '/processes',
      icon: <PlayCircleOutlined />,
      label: <Link to="/processes">Process Instances</Link>,
    },
    {
      key: '/designer',
      icon: <DesktopOutlined />,
      label: <Link to="/designer">Process Designer</Link>,
    },
    {
      key: '/forms',
      icon: <FormOutlined />,
      label: <Link to="/forms">Form Management</Link>,
    },
    {
      key: '/decisions',
      icon: <TableOutlined />,
      label: <Link to="/decisions">DMN Decisions</Link>,
    },
    {
      key: '/cases',
      icon: <FileTextOutlined />,
      label: <Link to="/cases">Case Management</Link>,
    },
    {
      key: '/legacy-decisions',
      icon: <ExperimentOutlined />,
      label: <Link to="/legacy-decisions">Legacy Decisions</Link>,
    },
    {
      key: '/monitoring',
      icon: <BarChartOutlined />,
      label: <Link to="/monitoring">Monitoring</Link>,
    },
  ];

  return (
    <Router>
      <Layout style={{ minHeight: '100vh' }}>
        <Header style={{ background: '#001529', padding: '0 24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', height: '100%' }}>
            <Title level={3} style={{ color: 'white', margin: 0 }}>
              Flowable UI
            </Title>
          </div>
        </Header>
        
        <Layout>
          <Sider width={200} style={{ background: '#fff' }}>
            <Menu
              mode="inline"
              defaultSelectedKeys={['/']}
              style={{ height: '100%', borderRight: 0 }}
              items={menuItems}
            />
          </Sider>
          
          <Layout style={{ padding: '0 24px 24px' }}>
            <Content style={{ background: '#fff', padding: 24, margin: 0, minHeight: 280 }}>
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/tasks" element={<TaskInbox />} />
                <Route path="/tasks/:taskId" element={<TaskDetail />} />
                <Route path="/processes" element={<Processes />} />
                <Route path="/designer" element={<ProcessDesigner />} />
                <Route path="/forms" element={<FormManagement />} />
                <Route path="/decisions" element={<DmnManagement />} />
                <Route path="/cases" element={<CaseManagement />} />
                <Route path="/legacy-decisions" element={<DecisionManagement />} />
                <Route path="/monitoring" element={<Monitoring />} />
              </Routes>
            </Content>
          </Layout>
        </Layout>
      </Layout>
    </Router>
  );
};

export default App;
