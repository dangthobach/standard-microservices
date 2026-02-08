
import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { processApi, historyApi } from '../../services/flowableApi';

const Monitor = () => {
    const { processInstanceId } = useParams<{ processInstanceId: string }>();
    const [variables, setVariables] = useState<any[]>([]);
    const [history, setHistory] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);

    // Default mock values if no ID provided
    const [caseId, setCaseId] = useState("#CASE-2201-XYZ");
    const [title, setTitle] = useState("Auto Insurance Claim");

    useEffect(() => {
        if (processInstanceId) {
            setLoading(true);
            const fetchData = async () => {
                try {
                    const vars = await processApi.getProcessVariables(processInstanceId);
                    const hist = await historyApi.getProcessHistory(processInstanceId);
                    setVariables(vars);
                    setHistory(hist);
                    setCaseId(`#${processInstanceId.substring(0, 8)}`);
                    setTitle("Active Process Instance");
                } catch (e) {
                    console.error("Failed to load process data", e);
                } finally {
                    setLoading(false);
                }
            };
            fetchData();
        }
    }, [processInstanceId]);

    // Check variables for dynamic status
    const getVariable = (name: String) => variables.find(v => v.name === name)?.value;

    return (
        <div className="bg-surface-highlight font-display text-text-main h-full flex overflow-hidden relative">
            <div className="flex-1 flex flex-col h-full min-w-0 overflow-hidden relative">
                <header className="bg-surface/80 backdrop-blur-sm border-b border-border-light flex-shrink-0 z-20 sticky top-0">
                    <div className="px-8 py-4 flex flex-col gap-2">
                        <div className="flex items-center gap-2 text-sm text-text-secondary">
                            <span className="hover:text-primary cursor-pointer transition-colors">Cases</span>
                            <span className="material-symbols-outlined text-[14px]">chevron_right</span>
                            <span className="hover:text-primary cursor-pointer transition-colors">{title}</span>
                            <span className="material-symbols-outlined text-[14px]">chevron_right</span>
                            <span className="text-text-main font-semibold bg-slate-100 px-2 py-0.5 rounded text-xs">{caseId}</span>
                        </div>
                        <div className="flex flex-wrap items-center justify-between gap-4">
                            <div className="flex items-center gap-4">
                                <div className="flex items-center justify-center size-12 rounded-xl bg-indigo-50 text-indigo-500 shadow-sm border border-indigo-100">
                                    <span className="material-symbols-outlined text-[28px]">folder_managed</span>
                                </div>
                                <div>
                                    <h1 className="text-text-main text-2xl font-bold tracking-tight">{title}</h1>
                                    <div className="flex items-center gap-3 mt-1">
                                        <span className="text-text-secondary text-sm">Created by <strong>Customer Portal</strong></span>
                                        <div className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-blue-50 border border-blue-100">
                                            <div className="size-1.5 rounded-full bg-blue-500"></div>
                                            <span className="text-blue-600 text-[10px] font-bold uppercase tracking-wide">Active</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div className="flex gap-3">
                                <button className="flex items-center gap-2 h-10 px-4 rounded-xl bg-white hover:bg-slate-50 text-text-secondary text-sm font-semibold border border-border-light shadow-sm transition-all">
                                    <span className="material-symbols-outlined text-[20px]">history</span>
                                    Audit Log
                                </button>
                                <button className="flex items-center gap-2 h-10 px-5 rounded-xl bg-primary hover:bg-blue-600 text-white text-sm font-semibold shadow-lg shadow-blue-500/20 transition-all">
                                    <span className="material-symbols-outlined text-[20px]">add_circle</span>
                                    Add Note
                                </button>
                            </div>
                        </div>
                    </div>
                </header>
                <div className="flex-1 overflow-hidden relative bg-slate-50 bg-dot-pattern">
                    <div className="absolute bottom-28 left-8 z-10 flex flex-col gap-2 bg-white border border-border-light p-1.5 rounded-xl shadow-float">
                        <button className="size-9 flex items-center justify-center text-text-secondary hover:bg-slate-50 hover:text-primary rounded-lg transition-colors" title="Zoom In">
                            <span className="material-symbols-outlined text-[20px]">add</span>
                        </button>
                        <button className="size-9 flex items-center justify-center text-text-secondary hover:bg-slate-50 hover:text-primary rounded-lg transition-colors" title="Zoom Out">
                            <span className="material-symbols-outlined text-[20px]">remove</span>
                        </button>
                        <button className="size-9 flex items-center justify-center text-text-secondary hover:bg-slate-50 hover:text-primary rounded-lg transition-colors" title="Fit to Screen">
                            <span className="material-symbols-outlined text-[20px]">center_focus_strong</span>
                        </button>
                    </div>
                    <div className="absolute inset-0 overflow-auto flex items-center justify-center p-12">
                        <div className="relative w-full max-w-[900px] min-h-[550px] bg-white rounded-3xl shadow-xl border border-slate-200 p-10 ring-1 ring-slate-100/50 overflow-x-auto">
                            <div className="absolute -top-4 left-10 bg-white px-4 py-1.5 rounded-full border border-slate-200 shadow-sm flex items-center gap-2">
                                <span className="material-symbols-outlined text-slate-400 text-[18px]">folder_open</span>
                                <span className="text-text-main font-bold text-sm tracking-tight">Case Plan Model</span>
                            </div>
                            <div className="grid grid-cols-12 gap-8 h-full">
                                {loading && <div className="col-span-12 text-center text-slate-400">Loading process state...</div>}
                                {!loading && (
                                    <>
                                        <div className="col-span-12 flex justify-between items-start mb-2">
                                            <div className="flex flex-col gap-1">
                                                <div className="h-9 px-4 rounded-full border border-emerald-200 bg-pastel-green flex items-center gap-2 shadow-sm">
                                                    <span className="material-symbols-outlined text-emerald-600 text-[20px]">flag</span>
                                                    <span className="text-emerald-700 text-xs font-bold uppercase">Start</span>
                                                </div>
                                                <span className="text-slate-400 text-[10px] ml-4">Completed</span>
                                            </div>
                                            {/* Dynamic Variables Display */}
                                            {variables.length > 0 && (
                                                <div className="flex flex-col gap-1 items-end">
                                                    <div className="text-xs font-bold text-slate-500 uppercase">Process Variables</div>
                                                    {variables.slice(0, 3).map((v, i) => (
                                                        <span key={i} className="text-[10px] bg-slate-100 px-1.5 py-0.5 rounded text-slate-600">
                                                            {v.name}: {JSON.stringify(v.value)}
                                                        </span>
                                                    ))}
                                                </div>
                                            )}
                                        </div>
                                        <div className="col-span-4 flex flex-col gap-4">
                                            <div className="border border-slate-200 bg-slate-50/80 rounded-2xl p-5 relative">
                                                <div className="absolute -top-2.5 left-4 bg-white px-2 py-0.5 rounded border border-slate-100 text-slate-500 text-[11px] font-bold shadow-sm">Stage 1</div>
                                                <div className="flex flex-col gap-3 mt-2 opacity-60 grayscale-[0.5]">
                                                    <div className="bg-white border border-slate-200 rounded-xl p-3 flex items-center gap-3 shadow-sm">
                                                        <span className="material-symbols-outlined text-emerald-500 text-[18px]">check_circle</span>
                                                        <span className="text-slate-500 text-sm font-medium line-through">Task A</span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="col-span-5 flex flex-col gap-4">
                                            <div className="border-2 border-primary/20 border-dashed bg-primary-soft rounded-2xl p-6 relative shadow-[0_0_30px_rgba(59,130,246,0.05)] transition-all ring-4 ring-blue-50/50">
                                                <div className="absolute -top-3 left-4 bg-white border border-blue-100 px-3 py-0.5 rounded-full text-primary text-sm font-bold flex items-center gap-1.5 shadow-sm z-10">
                                                    <span className="relative flex h-2 w-2">
                                                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                                                        <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                                                    </span>
                                                    Active Stage
                                                </div>
                                                <div className="flex flex-col gap-5 mt-2">
                                                    <div className="relative bg-white border border-primary/30 rounded-xl p-4 flex items-start gap-4 shadow-lg shadow-blue-500/5 group cursor-pointer hover:-translate-y-0.5 transition-transform duration-300">
                                                        <div className="absolute -left-[8px] top-1/2 -translate-y-1/2 sentry-diamond active z-10" title="Entry Criteria Met"></div>
                                                        <div className="bg-blue-50 p-2 rounded-lg text-primary">
                                                            <span className="material-symbols-outlined text-[24px]">person_search</span>
                                                        </div>
                                                        <div className="flex flex-col flex-1">
                                                            <div className="flex justify-between items-start">
                                                                <span className="text-text-main text-sm font-bold">Current Task</span>
                                                                <span className="material-symbols-outlined text-slate-300 text-[16px]" title="Manually Activated">play_circle</span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                    {/* Details Panel - Keeping functionality as is, but dynamically loading data if needed */}
                    <div className="absolute top-6 right-6 w-80 glass-panel rounded-2xl shadow-float flex flex-col overflow-hidden transition-all duration-300">
                        <div className="bg-white/50 border-b border-white/60 p-4 flex justify-between items-center">
                            <div className="flex items-center gap-2">
                                <div className="size-2 rounded-full bg-primary animate-pulse"></div>
                                <span className="text-sm font-bold text-text-main">Inspector</span>
                            </div>
                            <span className="text-[10px] font-medium text-slate-400 uppercase tracking-wider">Context: Process</span>
                        </div>
                        <div className="p-5 bg-gradient-to-b from-blue-50/50 to-transparent">
                            <h2 className="text-lg font-bold text-text-main mb-1">Details</h2>
                            <p className="text-xs text-text-secondary mb-3">Runtime information.</p>
                            <div className="flex flex-col gap-2">
                                {variables.map(v => (
                                    <div key={v.name} className="flex justify-between text-xs border-b border-slate-100 pb-1">
                                        <span className="font-medium text-slate-600">{v.name}</span>
                                        <span className="text-slate-500 truncate max-w-[120px]">{String(v.value)}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <style>{`
                .bg-dot-pattern {
                    background-image: radial-gradient(#cbd5e1 1px, transparent 1px);
                    background-size: 24px 24px;
                }
                .sentry-diamond {
                    width: 14px;
                    height: 14px;
                    background-color: #ffffff;
                    border: 2px solid #94a3b8;
                    transform: rotate(45deg);
                    transition: all 0.3s ease;
                }
                .sentry-diamond.active {
                    border-color: #3b82f6;
                    background-color: #3b82f6;
                    box-shadow: 0 0 10px rgba(59, 130, 246, 0.3);
                }
                .glass-panel {
                    background: rgba(255, 255, 255, 0.85);
                    backdrop-filter: blur(12px);
                    -webkit-backdrop-filter: blur(12px);
                    border: 1px solid rgba(255, 255, 255, 0.5);
                }
            `}</style>
        </div>
    );
};

export default Monitor;
