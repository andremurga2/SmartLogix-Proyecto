import React from 'react';
import { Navigate } from 'react-router-dom';

const ProtectedRoute = ({ children, requiredRole = null }) => {
    const isAuthenticated = localStorage.getItem('isAuthenticated');
    const userData = localStorage.getItem('user');

    if (!isAuthenticated || !userData) {
        return <Navigate to="/login" replace />;
    }

    if (requiredRole) {
        const user = JSON.parse(userData);
        if (user.role !== requiredRole) {
            return <Navigate to="/catalog" replace />;
        }
    }

    return children;
};

export default ProtectedRoute;
