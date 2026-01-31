import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import 'antd/dist/reset.css';

import TaskInbox from './components/TaskInbox';
import TaskDetail from './pages/TaskDetail';
import FormManagement from './pages/FormManagement';
import DmnManagement from './pages/DmnManagement';
import CaseManagement from './components/CaseManagement';
import DecisionManagement from './components/DecisionManagement';
import OperationsDashboard from './pages/ProcessManagement/Dashboard';
import ProcessMonitor from './pages/ProcessManagement/Monitor';
import DeploymentCenter from './pages/ProcessManagement/DeploymentCenter';
import VersionDiff from './pages/ProcessManagement/VersionDiff';

import ServiceCatalog from './pages/Integration/ServiceCatalog';
import AnalyticsDashboard from './pages/Analytics/AnalyticsDashboard';

const App: React.FC = () => {
  return (
    <Router>
      <div className="flex h-screen w-full overflow-hidden bg-surface-highlight font-body text-text-main">
        {/* Sidebar */}
        <aside className="w-20 flex-shrink-0 bg-surface border-r border-border-light flex flex-col items-center h-full z-30 shadow-[4px_0_24px_rgba(0,0,0,0.02)]">
          <div className="p-4 mb-4">
            <Link to="/">
              <div className="bg-primary flex items-center justify-center rounded-xl size-10 text-white shadow-lg shadow-primary/30 cursor-pointer hover:scale-105 transition-transform">
                <span className="material-symbols-outlined text-2xl">hub</span>
              </div>
            </Link>
          </div>

          <nav className="flex flex-col gap-4 flex-1 w-full px-3">
            <Link
              to="/process-management/dashboard"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">dashboard</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Dashboard</div>
            </Link>

            <Link
              to="/process-management/monitor"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">monitor_heart</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Monitor</div>
            </Link>

            <Link
              to="/process-management/deployments"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">rocket_launch</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Deployments</div>
            </Link>

            <div className="w-8 h-px bg-slate-100 mx-auto my-1"></div>

            <Link
              to="/tasks"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">check_box</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Tasks</div>
            </Link>

            <Link
              to="/forms"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">assignment</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Forms</div>
            </Link>

            <Link
              to="/decisions"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">alt_route</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Decisions</div>
            </Link>

            <div className="w-8 h-px bg-slate-100 mx-auto my-1"></div>

            <Link
              to="/analytics/dashboard"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">monitoring</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Analytics</div>
            </Link>

            <Link
              to="/integration/catalog"
              className="sidebar-item flex justify-center items-center size-12 rounded-xl text-text-secondary hover:bg-slate-50 hover:text-primary transition-colors group relative"
            >
              <span className="material-symbols-outlined text-[24px]">extension</span>
              <div className="absolute left-14 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-50">Integration</div>
            </Link>
          </nav>

          <div className="flex flex-col items-center gap-4 w-full px-3 mb-6">
            <div className="size-10 rounded-full bg-slate-200 border-2 border-white shadow-sm flex items-center justify-center text-slate-600 font-bold text-xs cursor-pointer hover:ring-2 hover:ring-primary hover:ring-offset-2 transition-all">
              AD
            </div>
          </div>
        </aside>

        {/* Main Content */}
        <main className="flex-1 flex flex-col h-full overflow-hidden relative">
          <Routes>
            <Route path="/" element={<OperationsDashboard />} />
            <Route path="/process-management/dashboard" element={<OperationsDashboard />} />
            <Route path="/process-management/monitor" element={<ProcessMonitor />} />
            <Route path="/process-management/monitor/:processInstanceId" element={<ProcessMonitor />} />
            <Route path="/process-management/deployments" element={<DeploymentCenter />} />
            <Route path="/process-management/deployments/:processDefinitionId/diff" element={<VersionDiff />} />
            <Route path="/tasks" element={<TaskInbox />} />
            <Route path="/tasks/:taskId" element={<TaskDetail />} />
            <Route path="/forms" element={<FormManagement />} />
            <Route path="/decisions" element={<DmnManagement />} />
            <Route path="/cases" element={<CaseManagement />} />
            <Route path="/legacy-decisions" element={<DecisionManagement />} />
            <Route path="/integration/catalog" element={<ServiceCatalog />} />
            <Route path="/analytics/dashboard" element={<AnalyticsDashboard />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
};

export default App;
