import React, { useEffect, useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import authService from '../services/authService';

const ProtectedRoute: React.FC = () => {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

    useEffect(() => {
        const checkAuth = async () => {
            const authenticated = await authService.isAuthenticated();
            setIsAuthenticated(authenticated);
            if (!authenticated) {
                // Optionally trigger login automatically
                // authService.login();
            }
        };
        checkAuth();
    }, []);

    if (isAuthenticated === null) {
        return <div>Loading authentication...</div>;
    }

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

export default ProtectedRoute;
