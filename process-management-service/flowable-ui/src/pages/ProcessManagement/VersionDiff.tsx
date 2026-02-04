import React, { useState, useEffect, useRef } from 'react';
import BpmnViewer from 'bpmn-js';
import { processApi } from '../../services/flowableApi';
import { ProcessDefinition } from '../../types';

const VersionDiff: React.FC = () => {
    const [viewMode, setViewMode] = useState<'split' | 'overlay'>('split');
    const [definitions, setDefinitions] = useState<ProcessDefinition[]>([]);
    const [leftVersion, setLeftVersion] = useState<string>('');
    const [rightVersion, setRightVersion] = useState<string>('');
    const [loading, setLoading] = useState(false);

    const leftContainerRef = useRef<HTMLDivElement>(null);
    const rightContainerRef = useRef<HTMLDivElement>(null);
    const leftViewerRef = useRef<BpmnViewer | null>(null);
    const rightViewerRef = useRef<BpmnViewer | null>(null);

    useEffect(() => {
        const fetchDefinitions = async () => {
            try {
                const data = await processApi.getProcesses();
                setDefinitions(data);
                // Group by key and find latest versions if needed, but for now just list all
                // Ideally we should group by Process Key and let user select Key first, then Versions.
                // For simplicity, we assume we are comparing versions of the same process if possible, or any process.

                if (data.length >= 2) {
                    setLeftVersion(data[0].id);
                    setRightVersion(data[1].id);
                } else if (data.length > 0) {
                    setLeftVersion(data[0].id);
                    setRightVersion(data[0].id);
                }
            } catch (error) {
                console.error("Failed to fetch definitions");
            }
        };
        fetchDefinitions();
    }, []);

    useEffect(() => {
        if (!leftContainerRef.current) return;
        leftViewerRef.current = new BpmnViewer({
            container: leftContainerRef.current,
            height: '100%',
            width: '100%'
        });

        return () => {
            leftViewerRef.current?.destroy();
        };
    }, []);

    useEffect(() => {
        if (!rightContainerRef.current) return;
        rightViewerRef.current = new BpmnViewer({
            container: rightContainerRef.current,
            height: '100%',
            width: '100%'
        });

        return () => {
            rightViewerRef.current?.destroy();
        };
    }, []);

    useEffect(() => {
        const loadLeft = async () => {
            if (!leftVersion || !leftViewerRef.current) return;
            try {
                const xml = await processApi.getProcessBpmn(leftVersion);
                await leftViewerRef.current.importXML(xml);
                const canvas = leftViewerRef.current.get('canvas') as any;
                canvas.zoom('fit-viewport');
            } catch (err) {
                console.error('Failed to render left diagram', err);
            }
        };
        loadLeft();
    }, [leftVersion]);

    useEffect(() => {
        const loadRight = async () => {
            if (!rightVersion || !rightViewerRef.current) return;
            try {
                const xml = await processApi.getProcessBpmn(rightVersion);
                await rightViewerRef.current.importXML(xml);
                const canvas = rightViewerRef.current.get('canvas') as any;
                canvas.zoom('fit-viewport');
            } catch (err) {
                console.error('Failed to render right diagram', err);
            }
        };
        loadRight();
    }, [rightVersion]);

    return (
        <div className="flex flex-col h-full overflow-hidden bg-background-light text-slate-900 font-display">
            {/* Top Navigation */}
            <header className="flex-none flex items-center justify-between whitespace-nowrap border-b border-solid border-slate-200 bg-white px-6 py-3 shadow-sm z-20">
                <div className="flex items-center gap-4">
                    <div className="size-10 bg-primary/10 rounded-lg flex items-center justify-center text-primary">
                        <span className="material-symbols-outlined text-[24px]">difference</span>
                    </div>
                    <div>
                        <h2 className="text-slate-900 text-lg font-bold leading-tight">Process Version Comparison</h2>
                        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
                            <span className="flex items-center gap-1"><span className="material-symbols-outlined text-[14px]">history</span> Compare Versions</span>
                            <span>•</span>
                            <span>BPMN 2.0</span>
                        </div>
                    </div>
                </div>
                {/* Center Toolbar Controls */}
                <div className="hidden md:flex items-center bg-slate-100 rounded-lg p-1 border border-slate-200">
                    <button
                        onClick={() => setViewMode('split')}
                        className={`px-3 py-1.5 rounded text-sm font-semibold flex items-center gap-2 transition-all ${viewMode === 'split' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-600 hover:bg-slate-200'}`}
                    >
                        <span className="material-symbols-outlined text-[18px]">splitscreen</span> Split View
                    </button>
                    <button
                        onClick={() => setViewMode('overlay')}
                        disabled
                        className={`px-3 py-1.5 rounded text-sm font-semibold flex items-center gap-2 transition-all opacity-50 cursor-not-allowed ${viewMode === 'overlay' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-600'}`}
                    >
                        <span className="material-symbols-outlined text-[18px]">layers</span> Overlay (Coming Soon)
                    </button>
                </div>
                <div className="flex items-center gap-3">
                    <button className="flex cursor-pointer items-center justify-center overflow-hidden rounded-lg h-9 px-4 bg-primary hover:bg-blue-600 text-white text-sm font-bold gap-2 transition-colors shadow-sm shadow-blue-500/20">
                        <span className="material-symbols-outlined text-[18px]">call_merge</span>
                        <span className="truncate">Merge Changes</span>
                    </button>
                </div>
            </header>

            {/* Main Content Area */}
            <main className="flex flex-1 overflow-hidden relative">
                {/* Left Panel: Base Version */}
                <section className="flex-1 flex flex-col min-w-0 border-r border-slate-200 relative group">
                    <div className="flex items-center justify-between px-4 py-2 bg-slate-50 border-b border-slate-200 z-10">
                        <div className="flex items-center gap-2">
                            <span className="w-2 h-2 rounded-full bg-slate-400"></span>
                            <select
                                value={leftVersion}
                                onChange={(e) => setLeftVersion(e.target.value)}
                                className="bg-transparent text-sm font-bold text-slate-700 border-none focus:ring-0 cursor-pointer py-1 pr-8 pl-0"
                            >
                                <option value="" disabled>Select Version</option>
                                {definitions.map(d => (
                                    <option key={d.id} value={d.id}>{d.name} (v{d.version}) {d.id === leftVersion ? '(Selected)' : ''}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <div className="flex-1 bg-white bg-grid-pattern relative overflow-hidden p-0">
                        <div ref={leftContainerRef} className="h-full w-full"></div>
                    </div>
                </section>

                {/* Middle Panel: Changes List (Static for now as strict diffing is complex) */}
                <aside className="w-80 flex-none flex flex-col bg-white border-r border-slate-200 shadow-xl z-20 hidden lg:flex">
                    <div className="p-4 border-b border-slate-200 flex items-center justify-between">
                        <h3 className="text-sm font-bold text-slate-900">Visual Diff</h3>
                        <span className="bg-primary/10 text-primary text-xs font-bold px-2 py-1 rounded-full">Info</span>
                    </div>
                    <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-2 text-sm text-slate-600">
                        <p>Select two versions to compare their diagrams side-by-side.</p>
                        <p>Visual differences must be identified manually in this current version.</p>
                    </div>
                </aside>

                {/* Right Panel: Target Version */}
                <section className="flex-1 flex flex-col min-w-0 relative group">
                    <div className="flex items-center justify-between px-4 py-2 bg-slate-50 border-b border-slate-200 z-10">
                        <div className="flex items-center gap-2">
                            <span className="w-2 h-2 rounded-full bg-primary"></span>
                            <select
                                value={rightVersion}
                                onChange={(e) => setRightVersion(e.target.value)}
                                className="bg-transparent text-sm font-bold text-slate-700 border-none focus:ring-0 cursor-pointer py-1 pr-8 pl-0"
                            >
                                <option value="" disabled>Select Version</option>
                                {definitions.map(d => (
                                    <option key={d.id} value={d.id}>{d.name} (v{d.version}) {d.id === rightVersion ? '(Selected)' : ''}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <div className="flex-1 bg-white bg-grid-pattern relative overflow-hidden p-0">
                        <div ref={rightContainerRef} className="h-full w-full"></div>
                    </div>
                </section>
            </main>

            {/* Styles */}
            <style>{`
                .custom-scrollbar::-webkit-scrollbar {
                    width: 6px;
                    height: 6px;
                }
                .custom-scrollbar::-webkit-scrollbar-track {
                    background: transparent;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb {
                    background-color: #cbd5e1;
                    border-radius: 3px;
                }
                .bg-grid-pattern {
                    background-image: radial-gradient(#cbd5e1 1px, transparent 1px);
                    background-size: 20px 20px;
                }
            `}</style>
        </div>
    );
};

export default VersionDiff;
