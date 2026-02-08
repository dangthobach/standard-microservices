
import React, { useEffect, useState } from 'react';
import { dashboardApi } from '../../services/flowableApi';

const Dashboard = () => {
    const [stats, setStats] = useState({
        runningInstances: 0,
        completedInstances24h: 0,
        failedJobs: 0,
        terminatedInstances: 0,
        systemHealth: 'OK',
        uptime: 'N/A'
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const data = await dashboardApi.getStats();
                setStats(data);
            } catch (error) {
                console.error("Failed to fetch dashboard stats", error);
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, []);

    return (
        <div className="flex-1 flex flex-col h-full overflow-hidden bg-surface-highlight relative font-body">
            <header className="flex-shrink-0 flex items-center justify-between bg-surface/80 backdrop-blur-md px-8 py-4 z-20 sticky top-0 border-b border-border-light">
                <div className="flex items-center gap-4">
                    <h2 className="text-text-main text-xl font-bold tracking-tight font-display">Operations Overview</h2>
                    <span className="px-2 py-1 rounded-md bg-slate-100 text-text-secondary text-xs font-medium">v3.12.0</span>
                </div>
                <div className="flex items-center gap-4">
                    <div className="relative group hidden md:block">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                            <span className="material-symbols-outlined text-text-light text-[20px]">search</span>
                        </div>
                        <input
                            className="block w-72 pl-10 pr-3 py-2 border border-border-light rounded-xl leading-5 bg-surface-highlight text-text-main placeholder-text-light focus:outline-none focus:bg-white focus:border-primary focus:ring-2 focus:ring-primary/20 sm:text-sm transition-all shadow-sm"
                            placeholder="Search by ID, Key, or Name..." type="text" />
                    </div>
                    <div className="flex items-center gap-2 pl-4 border-l border-border-light">
                        <button className="flex items-center justify-center size-10 rounded-xl text-text-secondary hover:text-primary hover:bg-primary-soft transition-colors relative">
                            <span className="material-symbols-outlined text-[22px]">notifications</span>
                            <span className="absolute top-2.5 right-2.5 size-2 bg-danger rounded-full ring-2 ring-white"></span>
                        </button>
                        <button
                            className="flex items-center justify-center size-10 rounded-xl text-text-secondary hover:text-primary hover:bg-primary-soft transition-colors"
                            onClick={() => window.location.reload()}
                        >
                            <span className="material-symbols-outlined text-[22px] animate-spin-slow">refresh</span>
                        </button>
                    </div>
                </div>
            </header>
            <div className="flex-1 overflow-y-auto p-8">
                <div className="max-w-[1600px] mx-auto flex flex-col gap-8">
                    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
                        <div className="group flex flex-col gap-4 rounded-2xl p-6 bg-surface hover:shadow-soft border border-transparent hover:border-primary/10 transition-all shadow-card">
                            <div className="flex justify-between items-start">
                                <div className="bg-blue-50 text-blue-600 p-2.5 rounded-xl group-hover:bg-blue-600 group-hover:text-white transition-colors">
                                    <span className="material-symbols-outlined block">play_circle</span>
                                </div>
                                <span className="text-emerald-600 text-sm font-semibold bg-emerald-50 border border-emerald-100 px-2 py-1 rounded-lg flex items-center">
                                    <span className="material-symbols-outlined text-base mr-0.5">trending_up</span> Live
                                </span>
                            </div>
                            <div className="flex flex-col">
                                <h3 className="text-text-main text-3xl font-bold font-display">
                                    {loading ? '...' : stats.runningInstances}
                                </h3>
                                <p className="text-text-secondary text-sm font-medium">Running Instances</p>
                            </div>
                        </div>
                        <div className="group flex flex-col gap-4 rounded-2xl p-6 bg-surface hover:shadow-soft border border-transparent hover:border-emerald-500/10 transition-all shadow-card">
                            <div className="flex justify-between items-start">
                                <div className="bg-emerald-50 text-emerald-600 p-2.5 rounded-xl group-hover:bg-emerald-600 group-hover:text-white transition-colors">
                                    <span className="material-symbols-outlined block">check_circle</span>
                                </div>
                                <span className="text-emerald-600 text-sm font-semibold bg-emerald-50 border border-emerald-100 px-2 py-1 rounded-lg flex items-center">
                                    <span className="material-symbols-outlined text-base mr-0.5">trending_up</span> 24h
                                </span>
                            </div>
                            <div className="flex flex-col">
                                <h3 className="text-text-main text-3xl font-bold font-display">
                                    {loading ? '...' : stats.completedInstances24h}
                                </h3>
                                <p className="text-text-secondary text-sm font-medium">Completed (24h)</p>
                            </div>
                        </div>
                        <div className="group flex flex-col gap-4 rounded-2xl p-6 bg-surface hover:shadow-soft border border-transparent hover:border-amber-500/10 transition-all shadow-card">
                            <div className="flex justify-between items-start">
                                <div className="bg-amber-50 text-amber-600 p-2.5 rounded-xl group-hover:bg-amber-500 group-hover:text-white transition-colors">
                                    <span className="material-symbols-outlined block">cancel</span>
                                </div>
                                <span className="text-emerald-600 text-sm font-semibold bg-emerald-50 border border-emerald-100 px-2 py-1 rounded-lg flex items-center">
                                    <span className="material-symbols-outlined text-base mr-0.5">trending_down</span> Total
                                </span>
                            </div>
                            <div className="flex flex-col">
                                <h3 className="text-text-main text-3xl font-bold font-display">
                                    {loading ? '...' : stats.terminatedInstances}
                                </h3>
                                <p className="text-text-secondary text-sm font-medium">Terminated</p>
                            </div>
                        </div>
                        <div className="group flex flex-col gap-4 rounded-2xl p-6 bg-surface hover:shadow-soft border border-transparent hover:border-red-500/10 transition-all shadow-card relative overflow-hidden">
                            <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/5 blur-2xl rounded-full -mr-10 -mt-10 pointer-events-none"></div>
                            <div className="flex justify-between items-start">
                                <div className="bg-red-50 text-red-600 p-2.5 rounded-xl group-hover:bg-red-600 group-hover:text-white transition-colors">
                                    <span className="material-symbols-outlined block">error</span>
                                </div>
                                <span className="text-red-600 text-sm font-semibold bg-red-50 border border-red-100 px-2 py-1 rounded-lg flex items-center">
                                    <span className="material-symbols-outlined text-base mr-0.5">trending_up</span> Attention
                                </span>
                            </div>
                            <div className="flex flex-col">
                                <h3 className="text-text-main text-3xl font-bold font-display">
                                    {loading ? '...' : stats.failedJobs}
                                </h3>
                                <p className="text-text-secondary text-sm font-medium">Failed Jobs</p>
                            </div>
                        </div>
                    </div>
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 h-full">
                        <div className="flex flex-col gap-8">
                            <div className="bg-surface rounded-2xl p-8 flex flex-col md:flex-row items-center gap-10 shadow-card border border-border-light">
                                <div className="flex flex-col gap-4 flex-1">
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-emerald-100 text-emerald-600 rounded-lg">
                                            <span className="material-symbols-outlined text-xl">monitor_heart</span>
                                        </div>
                                        <div>
                                            <h3 className="text-text-main text-lg font-bold font-display">System Health</h3>
                                            <p className="text-text-light text-xs font-medium uppercase tracking-wide">Infrastructure Status</p>
                                        </div>
                                    </div>
                                    <p className="text-text-secondary text-sm leading-relaxed">
                                        System status is {stats.systemHealth}. All engines are performing optimally.
                                    </p>
                                    <div className="flex gap-8 mt-2">
                                        <div className="flex flex-col gap-1">
                                            <span className="text-xs text-text-light uppercase font-bold tracking-wider">Database</span>
                                            <div className="flex items-center gap-2">
                                                <div className={`size-2 rounded-full ${stats.systemHealth === 'OK' ? 'bg-emerald-500' : 'bg-red-500'}`}></div>
                                                <span className="text-slate-700 font-mono text-sm font-medium">Connected</span>
                                            </div>
                                        </div>
                                        <div className="flex flex-col gap-1">
                                            <span className="text-xs text-text-light uppercase font-bold tracking-wider">Async Exec</span>
                                            <div className="flex items-center gap-2">
                                                <div className="size-2 rounded-full bg-blue-500"></div>
                                                <span className="text-slate-700 font-mono text-sm font-medium">Active</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div className="relative size-44 flex-shrink-0">
                                    <div className="absolute inset-0 rounded-full shadow-inner" style={{ background: 'conic-gradient(#10b981 0% 88%, #ffffff 88% 90%, #3b82f6 90% 98%, #ffffff 98% 100%)' }}></div>
                                    <div className="absolute inset-4 bg-white rounded-full flex flex-col items-center justify-center shadow-sm">
                                        <span className="text-4xl font-bold text-text-main font-display">99%</span>
                                        <span className="text-[11px] text-text-light uppercase font-bold tracking-wider mt-1">Uptime</span>
                                    </div>
                                </div>
                            </div>
                            <div className="bg-surface rounded-2xl p-8 flex flex-col gap-6 shadow-card border border-border-light flex-1">
                                <div className="flex justify-between items-center">
                                    <div>
                                        <h3 className="text-text-main text-lg font-bold font-display">Engine Load</h3>
                                        <p className="text-text-secondary text-sm">Distribution by definition type</p>
                                    </div>
                                    <div className="flex gap-4 text-xs font-medium bg-slate-50 px-3 py-2 rounded-lg border border-slate-100">
                                        <div className="flex items-center gap-2 text-slate-600"><span className="size-2.5 rounded-full bg-blue-500 shadow-sm shadow-blue-200"></span> BPMN</div>
                                        <div className="flex items-center gap-2 text-slate-600"><span className="size-2.5 rounded-full bg-indigo-400 shadow-sm shadow-indigo-200"></span> CMMN</div>
                                        <div className="flex items-center gap-2 text-slate-600"><span className="size-2.5 rounded-full bg-teal-400 shadow-sm shadow-teal-200"></span> DMN</div>
                                    </div>
                                </div>
                                <div className="flex-1 min-h-[220px] flex items-end justify-between gap-8 px-4 pt-8 pb-0 relative">
                                    <div className="absolute inset-0 flex flex-col justify-between pointer-events-none pb-0 px-4">
                                        <div className="w-full h-px bg-slate-100"></div>
                                        <div className="w-full h-px bg-slate-100"></div>
                                        <div className="w-full h-px bg-slate-100"></div>
                                        <div className="w-full h-px bg-slate-100"></div>
                                        <div className="w-full h-px bg-slate-200"></div>
                                    </div>
                                    <div className="w-full flex flex-col items-center gap-3 z-10 group cursor-pointer">
                                        <div className="relative w-full max-w-[80px] rounded-t-lg h-full flex items-end overflow-hidden">
                                            <div className="w-full bg-blue-500 hover:bg-blue-600 transition-all duration-500 shadow-lg shadow-blue-500/20 rounded-t-lg" style={{ height: '75%' }}></div>
                                        </div>
                                        <span className="text-xs font-bold text-text-secondary uppercase tracking-wide group-hover:text-blue-600 transition-colors">BPMN</span>
                                    </div>
                                    <div className="w-full flex flex-col items-center gap-3 z-10 group cursor-pointer">
                                        <div className="relative w-full max-w-[80px] rounded-t-lg h-full flex items-end overflow-hidden">
                                            <div className="w-full bg-indigo-400 hover:bg-indigo-500 transition-all duration-500 shadow-lg shadow-indigo-400/20 rounded-t-lg" style={{ height: '35%' }}></div>
                                        </div>
                                        <span className="text-xs font-bold text-text-secondary uppercase tracking-wide group-hover:text-indigo-500 transition-colors">CMMN</span>
                                    </div>
                                    <div className="w-full flex flex-col items-center gap-3 z-10 group cursor-pointer">
                                        <div className="relative w-full max-w-[80px] rounded-t-lg h-full flex items-end overflow-hidden">
                                            <div className="w-full bg-teal-400 hover:bg-teal-500 transition-all duration-500 shadow-lg shadow-teal-400/20 rounded-t-lg" style={{ height: '55%' }}></div>
                                        </div>
                                        <span className="text-xs font-bold text-text-secondary uppercase tracking-wide group-hover:text-teal-500 transition-colors">DMN</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="flex flex-col gap-8">
                            <div className="bg-surface rounded-2xl flex flex-col shadow-card border border-border-light overflow-hidden">
                                <div className="px-6 py-5 border-b border-border-light flex justify-between items-center bg-slate-50/50">
                                    <div className="flex items-center gap-3">
                                        <div className="bg-red-100 p-1.5 rounded text-red-600">
                                            <span className="material-symbols-outlined text-[20px] block">warning</span>
                                        </div>
                                        <h3 className="text-text-main text-base font-bold">Active Incidents</h3>
                                    </div>
                                    <span className="bg-red-50 text-red-600 border border-red-100 text-xs px-2.5 py-1 rounded-full font-bold shadow-sm">{stats.failedJobs} New</span>
                                </div>
                                <div className="flex flex-col divide-y divide-slate-100">
                                    {/* Mock items for incident details - hard to get specific error details without more complex query */}
                                    <div className="p-5 hover:bg-slate-50 transition-colors flex items-center justify-center text-slate-400 text-sm">
                                        {stats.failedJobs > 0 ? "Check Management Console for details" : "No active incidents"}
                                    </div>
                                </div>
                                <div className="px-4 py-3 border-t border-slate-100 bg-slate-50 text-center">
                                    <button className="text-xs font-bold text-primary hover:text-blue-700 transition-colors">View All Incidents</button>
                                </div>
                            </div>
                            <div className="bg-surface rounded-2xl flex flex-col shadow-card border border-border-light flex-1">
                                <div className="px-6 py-5 border-b border-border-light flex justify-between items-center">
                                    <h3 className="text-text-main text-base font-bold">Recent Activity</h3>
                                    <button className="text-text-light hover:text-text-secondary transition-colors">
                                        <span className="material-symbols-outlined text-[20px]">more_horiz</span>
                                    </button>
                                </div>
                                <div className="p-6">
                                    {/* This section would require a robust audit log API */}
                                    <div className="relative border-l-2 border-slate-100 ml-3 space-y-8 pb-2">
                                        <p className="text-slate-400 text-sm ml-6">Activity stream requires Audit Service</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
