import React, { useState, useEffect } from 'react';
import { analyticsApi } from '../../services/flowableApi';

const AnalyticsDashboard = () => {
    const [heatmap, setHeatmap] = useState<any[]>([]);
    const [bottlenecks, setBottlenecks] = useState<any[]>([]);

    useEffect(() => {
        const loadData = async () => {
            try {
                const hm = await analyticsApi.getDmnHeatmap();
                setHeatmap(hm);
                const bn = await analyticsApi.getBottlenecks();
                setBottlenecks(bn);
            } catch (e) {
                console.error("Failed to load analytics", e);
            }
        };
        loadData();
    }, []);

    const getHeatmapColor = (hits: number) => {
        if (hits > 1000) return 'bg-primary/90';
        if (hits > 500) return 'bg-primary/60';
        if (hits > 200) return 'bg-primary/30';
        if (hits > 50) return 'bg-primary/10';
        return 'bg-primary/5';
    };

    return (
        <div className="flex-1 flex flex-col h-full bg-surface-highlight overflow-hidden font-body relative text-text-main">
            <header className="h-16 border-b border-border-light bg-surface/80 backdrop-blur-md sticky top-0 z-10 px-8 flex items-center justify-between flex-shrink-0">
                <div className="flex items-center gap-2 text-sm text-text-light">
                    <a className="hover:text-primary transition-colors" href="#">Analytics</a>
                    <span>/</span>
                    <span className="text-text-main font-semibold">Decision Drift Analysis</span>
                </div>
                <div className="flex items-center gap-4">
                    <div className="flex items-center bg-surface-highlight border border-border-light rounded-lg px-3 py-1.5 gap-2">
                        <span className="material-symbols-outlined text-sm">calendar_today</span>
                        <span className="text-xs font-medium">Last 30 Days</span>
                    </div>
                    <button className="bg-primary text-white text-xs font-bold px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-primary-dark transition-all">
                        <span className="material-symbols-outlined text-sm">refresh</span> Refresh Analytics
                    </button>
                </div>
            </header>

            <div className="flex-1 overflow-y-auto p-8 space-y-8">
                {/* Page Heading */}
                <div className="flex flex-col gap-1">
                    <h1 className="text-3xl font-black tracking-tight text-text-main font-display">Advanced Analytics & Rule Drift</h1>
                    <p className="text-text-secondary">Real-time comparison of expected vs. actual rule hit distributions across active process engines.</p>
                </div>

                {/* Stats Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="bg-surface p-6 rounded-xl border border-border-light shadow-sm flex flex-col gap-2">
                        <div className="flex justify-between items-start">
                            <p className="text-sm font-medium text-text-light">Drift Score</p>
                            <span className="p-1.5 bg-green-100 text-green-700 rounded-md text-[10px] font-bold">+2.1%</span>
                        </div>
                        <p className="text-3xl font-bold font-display">12.4%</p>
                        <p className="text-[11px] text-text-light">Confidence Level: High</p>
                    </div>
                    <div className="bg-surface p-6 rounded-xl border border-border-light shadow-sm flex flex-col gap-2">
                        <div className="flex justify-between items-start">
                            <p className="text-sm font-medium text-text-light">Automation Rate</p>
                            <span className="p-1.5 bg-red-100 text-red-700 rounded-md text-[10px] font-bold">-0.5%</span>
                        </div>
                        <p className="text-3xl font-bold font-display">88.2%</p>
                        <p className="text-[11px] text-text-light">Target: 92.0%</p>
                    </div>
                    <div className="bg-surface p-6 rounded-xl border border-border-light shadow-sm flex flex-col gap-2">
                        <div className="flex justify-between items-start">
                            <p className="text-sm font-medium text-text-light">Avg Latency</p>
                            <span className="p-1.5 bg-red-100 text-red-700 rounded-md text-[10px] font-bold">-4%</span>
                        </div>
                        <p className="text-3xl font-bold font-display">42ms</p>
                        <p className="text-[11px] text-text-light">P99: 112ms</p>
                    </div>
                    <div className="bg-surface p-6 rounded-xl border border-border-light shadow-sm flex flex-col gap-2">
                        <div className="flex justify-between items-start">
                            <p className="text-sm font-medium text-text-light">SLA Risk Level</p>
                            <span className="p-1.5 bg-green-100 text-green-700 rounded-md text-[10px] font-bold">Low</span>
                        </div>
                        <p className="text-3xl font-bold font-display">Minimal</p>
                        <p className="text-[11px] text-text-light">ML Prediction: 2% breach</p>
                    </div>
                </div>

                {/* Main Chart Area */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Decision Drift Comparison */}
                    <div className="lg:col-span-2 bg-surface p-6 rounded-xl border border-border-light shadow-sm">
                        <div className="flex items-center justify-between mb-8">
                            <div>
                                <h3 className="font-bold text-lg text-text-main font-display">Decision Drift Analysis</h3>
                                <p className="text-sm text-text-light">Comparing Expected vs. Actual rule hit distributions</p>
                            </div>
                            <div className="flex gap-4">
                                <div className="flex items-center gap-2">
                                    <div className="size-3 rounded-full bg-primary/30"></div>
                                    <span className="text-xs text-text-light">Expected</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    <div className="size-3 rounded-full bg-primary"></div>
                                    <span className="text-xs text-text-light">Actual Hits</span>
                                </div>
                            </div>
                        </div>
                        {/* Visual Mockup of Chart */}
                        <div className="h-64 flex items-end justify-between gap-4 px-4 border-b border-l border-border-light pb-2">
                            {[
                                { rule: 'Rule-01', exp: 60, act: 52 },
                                { rule: 'Rule-02', exp: 40, act: 65 },
                                { rule: 'Rule-03', exp: 80, act: 85 },
                                { rule: 'Rule-04', exp: 30, act: 15 },
                                { rule: 'Rule-05', exp: 55, act: 60 },
                                { rule: 'Rule-06', exp: 90, act: 40 },
                            ].map((item, i) => (
                                <div key={i} className="flex-1 flex flex-col items-center gap-1 group">
                                    <div className="w-full flex items-end justify-center gap-1 h-full">
                                        <div className="w-4 bg-primary/20 rounded-t-sm transition-all duration-500 hover:bg-primary/30" style={{ height: `${item.exp}%` }}></div>
                                        <div className="w-4 bg-primary rounded-t-sm transition-all duration-500 hover:brightness-110" style={{ height: `${item.act}%` }}></div>
                                    </div>
                                    <span className="text-[10px] font-medium text-text-light">{item.rule}</span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Process Efficiency / ML Sidebar */}
                    <div className="bg-surface p-6 rounded-xl border border-border-light shadow-sm flex flex-col gap-6">
                        <div className="flex flex-col gap-1">
                            <h3 className="font-bold text-lg text-text-main font-display">Predictive Health</h3>
                            <p className="text-sm text-text-light">ML model: process-v4.2</p>
                        </div>
                        <div className="space-y-6">
                            <div className="space-y-2">
                                <div className="flex justify-between text-xs font-semibold">
                                    <span className="text-text-main">SLA Breach Risk</span>
                                    <span className="text-primary">12% Prob.</span>
                                </div>
                                <div className="h-2 w-full bg-surface-highlight rounded-full overflow-hidden">
                                    <div className="bg-primary h-full w-[12%] rounded-full"></div>
                                </div>
                                <p className="text-[10px] text-text-light">Stable compared to yesterday</p>
                            </div>
                            <div className="space-y-2">
                                <div className="flex justify-between text-xs font-semibold">
                                    <span className="text-text-main">Automation Confidence</span>
                                    <span className="text-primary">94.8%</span>
                                </div>
                                <div className="h-2 w-full bg-surface-highlight rounded-full overflow-hidden">
                                    <div className="bg-primary h-full w-[94%] rounded-full"></div>
                                </div>
                                <p className="text-[10px] text-text-light">Strong decision consistency</p>
                            </div>
                            <div className="p-4 bg-primary/5 rounded-lg border border-primary/20">
                                <div className="flex items-center gap-3 mb-2">
                                    <span className="material-symbols-outlined text-primary text-xl">psychology</span>
                                    <span className="text-xs font-bold text-primary">AI Insight</span>
                                </div>
                                <p className="text-[11px] leading-relaxed text-text-secondary">
                                    "Rule-06 is hitting 55.6% less frequently than modeled. High probability of input data shift in <strong>Regional Markets</strong> segment."
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Bottom Row: Heatmap & Tables */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 pb-12">
                    {/* Rule Heatmap */}
                    <div className="bg-surface p-6 rounded-xl border border-border-light shadow-sm">
                        <div className="flex justify-between items-center mb-6">
                            <div>
                                <h3 className="font-bold text-lg text-text-main font-display">DMN Execution Heatmap</h3>
                                <p className="text-sm text-text-light">Most active rules in Decision Tables</p>
                            </div>
                            <div className="flex items-center gap-2">
                                <span className="text-[10px] text-text-light">Idle</span>
                                <div className="w-24 h-2 rounded-full bg-gradient-to-r from-blue-50 to-primary"></div>
                                <span className="text-[10px] text-text-light">Hot</span>
                            </div>
                        </div>
                        <div className="grid grid-cols-10 gap-1.5">
                            {heatmap.length > 0 ? heatmap.map((cell, i) => (
                                <div
                                    key={i}
                                    className={`aspect-square rounded-[2px] border border-primary/10 hover:border-primary transition-all cursor-help ${getHeatmapColor(cell.hits)}`}
                                    title={`${cell.ruleId}: ${cell.hits} Hits`}
                                ></div>
                            )) : <p className="col-span-10 text-xs text-center p-4">Loading Data...</p>}
                        </div>
                        <div className="mt-4 flex justify-between">
                            <button className="text-[11px] font-bold text-primary hover:underline">Download Heatmap PDF</button>
                            <span className="text-[10px] text-text-light">Total Decisons: 1.2M (last 24h)</span>
                        </div>
                    </div>

                    {/* Process Performance Table */}
                    <div className="bg-surface p-6 rounded-xl border border-border-light shadow-sm overflow-hidden">
                        <div className=" mb-6">
                            <h3 className="font-bold text-lg text-text-main font-display">Active Performance Bottlenecks</h3>
                            <p className="text-sm text-text-light">BPMN Process Instances with &gt;2h delay</p>
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left">
                                <thead>
                                    <tr className="text-[10px] font-bold text-text-light uppercase tracking-wider border-b border-border-light">
                                        <th className="pb-3 px-2">Process ID</th>
                                        <th className="pb-3 px-2">Task Node</th>
                                        <th className="pb-3 px-2">Avg Time</th>
                                        <th className="pb-3 px-2">Deviance</th>
                                    </tr>
                                </thead>
                                <tbody className="text-sm">
                                    {bottlenecks.map((row, i) => (
                                        <tr key={i} className="border-b border-surface-highlight hover:bg-surface-highlight transition-colors">
                                            <td className="py-3 px-2 font-medium text-text-main">{row.id}</td>
                                            <td className="py-3 px-2 text-text-secondary">{row.node}</td>
                                            <td className="py-3 px-2 text-text-secondary">{row.time}</td>
                                            <td className={`py-3 px-2 font-bold ${row.color}`}>{row.dev}</td>
                                        </tr>
                                    ))}
                                    {bottlenecks.length === 0 && (
                                        <tr><td colSpan={4} className="py-4 text-center text-xs text-text-light">No bottlenecks found.</td></tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                        <div className="mt-4">
                            <a className="text-xs font-bold text-primary hover:underline" href="#">View Performance Report</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AnalyticsDashboard;

