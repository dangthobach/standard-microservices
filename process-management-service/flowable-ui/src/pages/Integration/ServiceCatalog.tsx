import React, { useState, useEffect } from 'react';
import { integrationApi } from '../../services/flowableApi';
import { message } from 'antd';

const ServiceCatalog = () => {
    const [connectors, setConnectors] = useState<any[]>([]);
    const [selectedConnector, setSelectedConnector] = useState<any>(null);
    const [activeTab, setActiveTab] = useState('Authentication');
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [loading, setLoading] = useState(false);

    const fetchConnectors = async () => {
        setLoading(true);
        try {
            const data = await integrationApi.getConnectors();
            if (data.length === 0) {
                // Fallback to initial seed data if empty, or just empty
                setConnectors(initialConnectors);
            } else {
                setConnectors(data.map(mapBackendToUi));
            }
        } catch (error) {
            message.error("Failed to load connectors");
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchConnectors();
    }, []);

    const mapBackendToUi = (c: any) => ({
        id: c.id,
        name: c.name,
        status: c.status || 'Draft',
        statusColor: c.status === 'Ready' ? 'emerald' : 'amber',
        description: c.description,
        type: c.type,
        icon: getIconForType(c.type),
        highlight: c.type === 'REST' // Example logic
    });

    const getIconForType = (type: string) => {
        if (type.includes('Kafka')) return 'hub';
        if (type.includes('Rabbit')) return 'chat_bubble';
        if (type.includes('REST')) return 'api';
        if (type.includes('RPC')) return 'bolt';
        return 'extension';
    };

    const handleSave = async () => {
        if (!selectedConnector) return;
        try {
            // Simple mock of saving existing one, in real app we'd bind form inputs
            const payload = {
                ...selectedConnector,
                status: 'Ready',
                configuration: "{}" // Mock config
            };
            await integrationApi.saveConnector(payload);
            message.success(`Saved ${selectedConnector.name}`);
            setDrawerOpen(false);
            fetchConnectors();
        } catch (e) {
            message.error("Failed to save");
        }
    };

    const initialConnectors = [
        {
            id: 'kafka',
            name: 'Apache Kafka',
            status: 'Ready',
            statusColor: 'emerald',
            description: 'High-throughput distributed event streaming for real-time data pipelines.',
            type: 'Messaging',
            icon: 'hub'
        },
        {
            id: 'rabbitmq',
            name: 'RabbitMQ',
            status: 'Ready',
            statusColor: 'emerald',
            description: 'Enterprise message broker for robust decoupled communication.',
            type: 'Messaging',
            icon: 'chat_bubble'
        },
        {
            id: 'rest',
            name: 'REST API',
            status: 'In Configuration',
            statusColor: 'amber',
            description: 'Universal HTTP-based service tasks with OAuth2 and JSON support.',
            type: 'Standard API',
            icon: 'api',
            highlight: true
        },
        {
            id: 'grpc',
            name: 'gRPC Connector',
            status: 'Ready',
            statusColor: 'emerald',
            description: 'Modern, open source high performance Remote Procedure Call framework.',
            type: 'RPC Services',
            icon: 'bolt'
        }
    ];

    const openDrawer = (connector: any) => {
        setSelectedConnector(connector);
        setDrawerOpen(true);
    };

    return (
        <div className="flex-1 flex flex-col h-full bg-surface-highlight overflow-hidden font-body relative">
            <header className="flex-shrink-0 flex items-center justify-between bg-surface/80 backdrop-blur-md px-8 py-4 z-20 sticky top-0 border-b border-border-light">
                <div className="flex items-center gap-4">
                    <h2 className="text-text-main text-xl font-bold tracking-tight font-display">Service & Integration Catalog</h2>
                    <p className="text-text-secondary text-sm">Manage and configure reusable connectors</p>
                </div>
                <button className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg font-bold hover:bg-primary-dark transition-all shadow-sm">
                    <span className="material-symbols-outlined text-lg">add</span>
                    <span>Add New Connector</span>
                </button>
            </header>

            <div className="flex flex-1 overflow-hidden">
                {/* Sidebar */}
                <aside className="w-64 border-r border-border-light bg-surface p-6 flex flex-col gap-6 overflow-y-auto hidden lg:flex">
                    <div>
                        <h3 className="text-xs font-semibold text-text-light uppercase tracking-wider mb-4">Categories</h3>
                        <div className="flex flex-col gap-1">
                            <div className="flex items-center gap-3 px-3 py-2 rounded-lg bg-primary/10 text-primary font-medium">
                                <span className="material-symbols-outlined text-xl">grid_view</span>
                                <p className="text-sm">All Connectors</p>
                            </div>
                            <div className="flex items-center gap-3 px-3 py-2 rounded-lg text-text-secondary hover:bg-surface-highlight transition-colors cursor-pointer">
                                <span className="material-symbols-outlined text-xl">chat</span>
                                <p className="text-sm font-medium">Messaging</p>
                            </div>
                            <div className="flex items-center gap-3 px-3 py-2 rounded-lg text-text-secondary hover:bg-surface-highlight transition-colors cursor-pointer">
                                <span className="material-symbols-outlined text-xl">api</span>
                                <p className="text-sm font-medium">REST & gRPC</p>
                            </div>
                            <div className="flex items-center gap-3 px-3 py-2 rounded-lg text-text-secondary hover:bg-surface-highlight transition-colors cursor-pointer">
                                <span className="material-symbols-outlined text-xl">database</span>
                                <p className="text-sm font-medium">Storage</p>
                            </div>
                            <div className="flex items-center gap-3 px-3 py-2 rounded-lg text-text-secondary hover:bg-surface-highlight transition-colors cursor-pointer">
                                <span className="material-symbols-outlined text-xl">webhook</span>
                                <p className="text-sm font-medium">Webhooks</p>
                            </div>
                        </div>
                    </div>
                </aside>

                {/* Main Content */}
                <main className="flex-1 overflow-y-auto p-8 bg-surface-highlight">
                    <div className="flex gap-2 flex-wrap mb-6">
                        <div className="px-4 py-1.5 rounded-full bg-primary text-white text-xs font-bold cursor-pointer">All</div>
                        <div className="px-4 py-1.5 rounded-full bg-surface border border-border-light text-text-secondary text-xs font-semibold hover:border-primary transition-colors cursor-pointer">Messaging</div>
                        <div className="px-4 py-1.5 rounded-full bg-surface border border-border-light text-text-secondary text-xs font-semibold hover:border-primary transition-colors cursor-pointer">Rest API</div>
                        <div className="px-4 py-1.5 rounded-full bg-surface border border-border-light text-text-secondary text-xs font-semibold hover:border-primary transition-colors cursor-pointer">gRPC</div>
                        <div className="px-4 py-1.5 rounded-full bg-surface border border-border-light text-text-secondary text-xs font-semibold hover:border-primary transition-colors cursor-pointer">Webhooks</div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-6 pb-12">
                        {connectors.map(connector => (
                            <div
                                key={connector.id}
                                onClick={() => openDrawer(connector)}
                                className={`group bg-surface border ${connector.highlight ? 'border-primary ring-4 ring-primary/10' : 'border-border-light'} rounded-xl p-5 hover:border-primary hover:shadow-lg transition-all flex flex-col cursor-pointer relative`}
                            >
                                <div className={`absolute top-4 right-4 flex items-center justify-center ${connector.highlight ? 'text-primary' : 'text-text-light group-hover:text-primary'} transition-colors`}>
                                    <span className="material-symbols-outlined">drag_indicator</span>
                                </div>
                                <div className={`size-14 ${connector.highlight ? 'bg-primary/10 text-primary' : 'bg-surface-highlight text-text-secondary'} rounded-lg mb-4 flex items-center justify-center overflow-hidden`}>
                                    <span className="material-symbols-outlined text-3xl">{connector.icon}</span>
                                </div>
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <h3 className="text-lg font-bold text-text-main">{connector.name}</h3>
                                        <span className={`px-2 py-0.5 bg-${connector.statusColor}-100 text-${connector.statusColor}-600 text-[10px] font-bold rounded uppercase`}>
                                            {connector.status}
                                        </span>
                                    </div>
                                    <p className="text-sm text-text-secondary mb-4 leading-relaxed">{connector.description}</p>
                                    <div className="mt-auto pt-4 border-t border-border-light flex items-center justify-between">
                                        <span className="text-xs text-text-light">{connector.type}</span>
                                        <button className="text-xs font-bold text-primary hover:underline">Configure</button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </main>

                {/* Configuration Drawer */}
                {drawerOpen && selectedConnector && (
                    <aside className="w-[420px] bg-surface border-l border-border-light flex flex-col shadow-2xl relative z-40 animate-slide-in-right">
                        <div className="p-6 border-b border-border-light">
                            <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-3">
                                    <div className="size-10 bg-primary/10 text-primary rounded-lg flex items-center justify-center">
                                        <span className="material-symbols-outlined">{selectedConnector.icon}</span>
                                    </div>
                                    <div>
                                        <h2 className="text-lg font-bold text-text-main leading-none">{selectedConnector.name}</h2>
                                        <span className="text-[10px] text-text-light uppercase tracking-widest font-bold">{selectedConnector.type}</span>
                                    </div>
                                </div>
                                <button className="text-text-light hover:text-text-secondary" onClick={() => setDrawerOpen(false)}>
                                    <span className="material-symbols-outlined">close</span>
                                </button>
                            </div>
                            <div className="flex gap-6 mt-4">
                                {['Authentication', 'Endpoints', 'Schema'].map(tab => (
                                    <button
                                        key={tab}
                                        onClick={() => setActiveTab(tab)}
                                        className={`text-sm font-bold pb-2 transition-colors ${activeTab === tab ? 'border-b-2 border-primary text-primary' : 'text-text-secondary hover:text-text-main'}`}
                                    >
                                        {tab}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-6">
                            {/* Auth Selection */}
                            <div className="flex flex-col gap-2">
                                <label className="text-xs font-bold text-text-light uppercase">Auth Strategy</label>
                                <select className="w-full rounded-lg border-border-light bg-surface text-sm focus:border-primary focus:ring-primary py-2 px-3 outline-none border">
                                    <option>OAuth 2.0 (Authorization Code)</option>
                                    <option>OAuth 2.0 (Client Credentials)</option>
                                    <option>API Key (Header)</option>
                                    <option>Bearer Token</option>
                                    <option>Basic Auth</option>
                                </select>
                            </div>

                            <div className="flex flex-col gap-4 bg-surface-highlight p-4 rounded-xl border border-border-light">
                                <div className="flex flex-col gap-2">
                                    <label className="text-xs font-bold text-text-light uppercase">Client ID</label>
                                    <input className="w-full rounded-lg border-border-light bg-surface text-sm focus:border-primary focus:ring-primary py-2 px-3 border outline-none" type="text" defaultValue="pe_catalog_rest_01" />
                                </div>
                                <div className="flex flex-col gap-2 relative">
                                    <label className="text-xs font-bold text-text-light uppercase">Client Secret</label>
                                    <input className="w-full rounded-lg border-border-light bg-surface text-sm focus:border-primary focus:ring-primary py-2 px-3 border outline-none" type="password" defaultValue="••••••••••••••••" />
                                    <button className="absolute bottom-2.5 right-3 text-text-light">
                                        <span className="material-symbols-outlined text-sm">visibility</span>
                                    </button>
                                </div>
                                <div className="flex flex-col gap-2">
                                    <label className="text-xs font-bold text-text-light uppercase">Scopes</label>
                                    <input className="w-full rounded-lg border-border-light bg-surface text-sm focus:border-primary focus:ring-primary py-2 px-3 border outline-none" placeholder="read:write user:profile" type="text" />
                                    <p className="text-[10px] text-text-light">Space-separated list of scopes required for the service.</p>
                                </div>
                            </div>

                            <div className="flex flex-col gap-2">
                                <label className="text-xs font-bold text-text-light uppercase">Token Endpoint</label>
                                <input className="w-full rounded-lg border-border-light bg-surface text-sm focus:border-primary focus:ring-primary py-2 px-3 border outline-none" type="text" defaultValue="https://api.example.com/oauth/token" />
                            </div>

                            <div className="flex items-center justify-between p-3 bg-surface-highlight rounded-lg">
                                <div className="flex flex-col">
                                    <span className="text-sm font-bold text-text-main">Strict SSL</span>
                                    <span className="text-xs text-text-light">Require valid certificates</span>
                                </div>
                                <div className="w-10 h-5 bg-primary rounded-full relative cursor-pointer">
                                    <div className="absolute right-0.5 top-0.5 size-4 bg-white rounded-full shadow-sm"></div>
                                </div>
                            </div>
                        </div>

                        <div className="p-6 border-t border-border-light flex gap-3">
                            <button className="flex-1 px-4 py-2.5 bg-surface-highlight text-text-main rounded-lg font-bold text-sm hover:bg-slate-200 transition-colors">Test Connection</button>
                            <button
                                className="flex-1 px-4 py-2.5 bg-primary text-white rounded-lg font-bold text-sm hover:bg-primary-dark shadow-lg shadow-primary/20 transition-all"
                                onClick={handleSave}
                            >
                                Save Changes
                            </button>
                        </div>
                    </aside>
                )}
            </div>
        </div>
    );
};

export default ServiceCatalog;
