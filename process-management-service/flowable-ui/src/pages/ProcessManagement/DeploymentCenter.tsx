
import React, { useState, useEffect, useRef } from 'react';
import { processApi } from '../../services/flowableApi';

interface Deployment {
    id: string;
    name: string;
    version: number;
    category: string;
    deploymentTime: string;
}

const DeploymentCenter: React.FC = () => {
    const [deployments, setDeployments] = useState<Deployment[]>([]);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [deploymentName, setDeploymentName] = useState('');
    const [deploymentCategory, setDeploymentCategory] = useState('');
    const [loading, setLoading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const fetchDeployments = async () => {
        try {
            const data = await processApi.getDeployments();
            setDeployments(data);
        } catch (error) {
            console.error('Failed to fetch deployments', error);
        }
    };

    useEffect(() => {
        fetchDeployments();
    }, []);

    const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files[0]) {
            const file = event.target.files[0];
            setSelectedFile(file);
            setDeploymentName(file.name.split('.')[0]); // Default name from filename
            setUploadProgress(0);

            // Simulate validation progress
            const interval = setInterval(() => {
                setUploadProgress(prev => {
                    if (prev >= 100) {
                        clearInterval(interval);
                        return 100;
                    }
                    return prev + 10;
                });
            }, 100);
        }
    };

    const handleDeploy = async () => {
        if (!selectedFile || !deploymentName) {
            alert('Please fill in all fields (File, Name)');
            return;
        }

        setLoading(true);
        try {
            await processApi.deployProcess(selectedFile, deploymentName, deploymentCategory || 'Uncategorized');
            alert('Deployment successful');
            setSelectedFile(null);
            setDeploymentName('');
            setDeploymentCategory('');
            setUploadProgress(0);
            fetchDeployments();
        } catch (error) {
            console.error(error);
            alert('Deployment failed');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!window.confirm('Are you sure you want to delete this deployment?')) return;
        try {
            await processApi.deleteDeployment(id);
            fetchDeployments();
        } catch (error) {
            alert('Failed to delete deployment');
        }
    };

    const triggerFileInput = () => {
        fileInputRef.current?.click();
    };

    return (
        <div className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 h-full overflow-y-auto bg-surface-highlight">
            {/* Breadcrumbs */}
            <nav className="flex text-sm font-medium text-text-secondary">
                <a className="hover:text-primary transition-colors cursor-pointer">Home</a>
                <span className="mx-2">/</span>
                <span className="text-text-main">Deployments</span>
            </nav>

            {/* Page Heading */}
            <div className="space-y-2">
                <h2 className="text-3xl font-black tracking-tight text-text-main font-display">Workflow Deployment Center</h2>
                <p className="text-text-secondary text-lg max-w-2xl">Manage your business process definitions. Upload, validate, and deploy BPMN, DMN, and CMMN files directly to the engine.</p>
            </div>

            {/* Layout Grid: Stacked */}
            <div className="grid grid-cols-1 gap-8">
                {/* Upload & Staging Section */}
                <section className="bg-surface rounded-xl border border-border-light shadow-sm overflow-hidden">
                    <div className="p-6 border-b border-border-light flex justify-between items-center bg-white">
                        <h3 className="text-lg font-bold text-text-main">New Deployment</h3>
                        <span className="text-xs font-semibold px-2 py-1 bg-blue-50 text-primary rounded border border-blue-100">Staging Area</span>
                    </div>
                    <div className="p-6 space-y-6 bg-white">
                        {/* Drop Zone */}
                        {!selectedFile ? (
                            <div
                                onClick={triggerFileInput}
                                className="group relative flex flex-col items-center gap-4 rounded-xl border-2 border-dashed border-gray-300 bg-gray-50/50 px-6 py-12 transition-all hover:border-primary hover:bg-blue-50/30 cursor-pointer"
                            >
                                <div className="size-12 rounded-full bg-white shadow-sm flex items-center justify-center text-primary mb-2">
                                    <span className="material-symbols-outlined text-3xl">cloud_upload</span>
                                </div>
                                <div className="text-center space-y-1">
                                    <p className="text-text-main font-bold text-lg">Click to upload BPMN file</p>
                                    <p className="text-text-secondary text-sm">Supported formats: BPMN, DMN, CMMN (Max 10MB)</p>
                                </div>
                                <input
                                    type="file"
                                    ref={fileInputRef}
                                    className="hidden"
                                    accept=".bpmn,.xml,.dmn,.cmmn,.zip,.bar"
                                    onChange={handleFileSelect}
                                />
                            </div>
                        ) : (
                            /* Active Upload / Staging Item */
                            <div className="space-y-4">
                                <div className="text-sm font-bold text-text-secondary uppercase tracking-wider">Processing Queue</div>
                                {/* File Card: Validating */}
                                <div className="flex flex-col sm:flex-row gap-6 p-6 rounded-xl border border-border-light bg-white shadow-sm items-start sm:items-center">
                                    <div className="flex items-center justify-center size-12 rounded-xl bg-orange-50 text-orange-600 shrink-0">
                                        <span className="material-symbols-outlined text-[24px]">description</span>
                                    </div>
                                    <div className="flex-1 w-full space-y-3">
                                        <div className="flex justify-between items-center">
                                            <h4 className="font-bold text-text-main text-lg">{selectedFile.name}</h4>
                                            <span className="text-sm font-medium text-text-main">{uploadProgress}%</span>
                                        </div>
                                        {/* Progress Bar */}
                                        <div className="h-2 w-full rounded-full bg-gray-100 overflow-hidden">
                                            <div className="h-full rounded-full bg-primary transition-all duration-300" style={{ width: `${uploadProgress}%` }}></div>
                                        </div>

                                        {/* Inputs for Name/Category */}
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-2">
                                            <div>
                                                <label className="block text-xs font-semibold text-text-secondary mb-1">Process Name</label>
                                                <input
                                                    type="text"
                                                    value={deploymentName}
                                                    onChange={(e) => setDeploymentName(e.target.value)}
                                                    className="w-full text-sm rounded-lg border-border-light focus:border-primary focus:ring-primary"
                                                    placeholder="Enter process name"
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-xs font-semibold text-text-secondary mb-1">Category</label>
                                                <input
                                                    type="text"
                                                    value={deploymentCategory}
                                                    onChange={(e) => setDeploymentCategory(e.target.value)}
                                                    className="w-full text-sm rounded-lg border-border-light focus:border-primary focus:ring-primary"
                                                    placeholder="e.g. Finance"
                                                />
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-2 text-xs text-green-600 font-medium">
                                            <span className="material-symbols-outlined text-[16px]">check_circle</span>
                                            <span>Structure validated successfully</span>
                                        </div>
                                    </div>
                                    <div className="flex flex-col gap-2 w-full sm:w-auto mt-2 sm:mt-0">
                                        <button
                                            onClick={handleDeploy}
                                            disabled={loading || uploadProgress < 100}
                                            className="h-10 px-6 bg-primary hover:bg-blue-600 text-white text-sm font-bold rounded-xl transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                        >
                                            {loading ? <span className="material-symbols-outlined animate-spin text-[18px]">progress_activity</span> : null}
                                            Deploy
                                        </button>
                                        <button
                                            onClick={() => setSelectedFile(null)}
                                            className="h-10 px-6 text-text-secondary hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors flex items-center justify-center gap-2 bg-gray-50"
                                        >
                                            <span className="material-symbols-outlined text-[18px]">delete</span>
                                            Remove
                                        </button>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </section>

                {/* Deployment History Section */}
                <section className="space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                        <h3 className="text-xl font-bold text-text-main">Deployment History</h3>
                        {/* Filters */}
                        <div className="flex items-center gap-2">
                            <div className="relative">
                                <select className="appearance-none h-10 pl-3 pr-8 bg-white border border-border-light rounded-xl text-sm font-medium text-text-main focus:outline-none focus:ring-2 focus:ring-primary/20 shadow-sm">
                                    <option>All Types</option>
                                    <option>BPMN</option>
                                    <option>DMN</option>
                                    <option>CMMN</option>
                                </select>
                                <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-text-secondary text-[20px] pointer-events-none">expand_more</span>
                            </div>
                            <div className="relative">
                                <input className="h-10 pl-10 pr-4 bg-white border border-border-light rounded-xl text-sm font-medium text-text-main focus:outline-none focus:ring-2 focus:ring-primary/20 w-full sm:w-64 shadow-sm placeholder:text-text-light" placeholder="Search workflows..." type="text" />
                                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary text-[20px] pointer-events-none">search</span>
                            </div>
                        </div>
                    </div>
                    {/* Table Card */}
                    <div className="bg-surface rounded-xl border border-border-light shadow-sm overflow-hidden bg-white">
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm whitespace-nowrap">
                                <thead className="bg-gray-50 border-b border-border-light">
                                    <tr>
                                        <th className="px-6 py-4 font-semibold text-text-secondary">Name</th>
                                        <th className="px-6 py-4 font-semibold text-text-secondary">Category</th>
                                        <th className="px-6 py-4 font-semibold text-text-secondary">Status</th>
                                        <th className="px-6 py-4 font-semibold text-text-secondary">Deployment Date</th>
                                        <th className="px-6 py-4 font-semibold text-text-secondary text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-border-light">
                                    {deployments.length === 0 ? (
                                        <tr>
                                            <td colSpan={5} className="px-6 py-8 text-center text-text-secondary">
                                                No deployments found. Upload a file above to get started.
                                            </td>
                                        </tr>
                                    ) : (
                                        deployments.map((deploy) => (
                                            <tr key={deploy.id} className="hover:bg-gray-50/50 transition-colors group">
                                                <td className="px-6 py-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="size-8 rounded bg-blue-50 text-primary flex items-center justify-center">
                                                            <span className="material-symbols-outlined text-[20px]">account_tree</span>
                                                        </div>
                                                        <span className="font-bold text-text-main">{deploy.name}</span>
                                                    </div>
                                                </td>
                                                <td className="px-6 py-4 text-text-secondary">{deploy.category || 'Uncategorized'}</td>
                                                <td className="px-6 py-4">
                                                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-green-100 text-green-700 border border-green-200">
                                                        <span className="size-1.5 rounded-full bg-green-600"></span>
                                                        Active
                                                    </span>
                                                </td>
                                                <td className="px-6 py-4 text-text-secondary">{new Date(deploy.deploymentTime).toLocaleDateString()} {new Date(deploy.deploymentTime).toLocaleTimeString()}</td>
                                                <td className="px-6 py-4 text-right">
                                                    <button
                                                        onClick={() => handleDelete(deploy.id)}
                                                        className="text-text-secondary hover:text-red-500 p-1 rounded hover:bg-red-50 transition-colors"
                                                        title="Delete Deployment"
                                                    >
                                                        <span className="material-symbols-outlined">delete</span>
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                        {/* Pagination */}
                        <div className="border-t border-border-light bg-gray-50 px-6 py-3 flex items-center justify-between">
                            <p className="text-sm text-text-secondary">Showing <span className="font-bold text-text-main">1-{deployments.length}</span> of <span className="font-bold text-text-main">{deployments.length}</span> deployments</p>
                            <div className="flex gap-2">
                                <button className="px-3 py-1 text-sm font-medium text-text-secondary bg-white border border-border-light rounded hover:bg-gray-50 disabled:opacity-50" disabled>Previous</button>
                                <button className="px-3 py-1 text-sm font-medium text-text-secondary bg-white border border-border-light rounded hover:bg-gray-50">Next</button>
                            </div>
                        </div>
                    </div>
                </section>
            </div>
        </div>
    );
};

export default DeploymentCenter;
